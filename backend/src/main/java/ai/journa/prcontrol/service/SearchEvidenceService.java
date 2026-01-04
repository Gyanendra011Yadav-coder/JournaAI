package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.SearchProviderProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.UserProfile;
import ai.journa.prcontrol.repository.UserProfileRepository;
import ai.journa.prcontrol.service.integration.GoogleSearchService;
import ai.journa.prcontrol.service.integration.GoogleSearchService.SearchResult;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SearchEvidenceService {
  private static final Logger logger = LoggerFactory.getLogger(SearchEvidenceService.class);
  private static final int MAX_EVIDENCE_PAGES = 3;
  private static final int MAX_PAGE_TEXT_CHARS = 1200;
  private static final int MAX_LINK_HINTS = 6;

  private final IntegrationSettingsService integrationSettingsService;
  private final SearchProviderProperties searchProviderProperties;
  private final GoogleSearchService googleSearchService;
  private final HtmlFetchService htmlFetchService;
  private final CurrentUserService currentUserService;
  private final UserProfileRepository userProfileRepository;

  public SearchEvidenceService(IntegrationSettingsService integrationSettingsService,
                               SearchProviderProperties searchProviderProperties,
                               GoogleSearchService googleSearchService,
                               HtmlFetchService htmlFetchService,
                               CurrentUserService currentUserService,
                               UserProfileRepository userProfileRepository) {
    this.integrationSettingsService = integrationSettingsService;
    this.searchProviderProperties = searchProviderProperties;
    this.googleSearchService = googleSearchService;
    this.htmlFetchService = htmlFetchService;
    this.currentUserService = currentUserService;
    this.userProfileRepository = userProfileRepository;
  }

  public Optional<SearchEvidence> buildEvidence(Journalist journalist, List<Article> recentArticles, String skipUrl) {
    if (journalist == null || journalist.getFullName() == null || journalist.getFullName().isBlank()) {
      return Optional.empty();
    }
    logger.info("Search evidence start journalistId={} name={} publication={} recentArticles={} skipUrl={}",
        journalist.getId(),
        journalist.getFullName(),
        journalist.getPublicationName(),
        recentArticles != null ? recentArticles.size() : 0,
        skipUrl);
    IntegrationSettings settings;
    try {
      settings = integrationSettingsService.getSettings(ProviderType.GOOGLE_CSE);
    } catch (Exception ex) {
      logger.warn("Google CSE settings missing reason={}", ex.getMessage());
      return Optional.empty();
    }
    if (settings == null || !settings.isEnabled()) {
      logger.info("Google CSE search skipped reason=DISABLED");
      return Optional.empty();
    }
    String apiKey = integrationSettingsService.resolveApiKey(settings);
    if (apiKey == null || apiKey.isBlank()) {
      logger.warn("Google CSE skipped reason=NO_API_KEY");
      return Optional.empty();
    }
    String searchEngineId = integrationSettingsService.resolveSearchEngineId(settings);
    if (searchEngineId == null || searchEngineId.isBlank()) {
      logger.warn("Google CSE skipped reason=NO_SEARCH_ENGINE_ID");
      return Optional.empty();
    }
    String articleTitle = extractArticleTitle(recentArticles);
    List<String> queries = buildQueryVariants(journalist, articleTitle);
    LocalePreference preference = resolveLocaleFromProfile();
    String lang = preference.lang();
    String country = preference.country();
    if (lang == null || lang.isBlank()) {
      lang = searchProviderProperties.getGoogle().getDefaultLang();
    }
    if (country == null || country.isBlank()) {
      country = searchProviderProperties.getGoogle().getDefaultCountry();
    }
    logger.info("Search evidence locale source={} lang={} country={} articleTitle={}",
        preference.source(),
        lang,
        country,
        articleTitle);
    int maxPerRequest = settings.getMaxPerRequest() != null
        ? settings.getMaxPerRequest()
        : searchProviderProperties.getGoogle().getMaxResults();
    boolean hasLocale = (lang != null && !lang.isBlank()) || (country != null && !country.isBlank());
    List<SearchResult> results = List.of();
    String queryUsed = null;
    boolean usedLocale = true;
    for (String candidate : queries) {
      results = googleSearchService.search(candidate, apiKey, searchEngineId, maxPerRequest, lang, country);
      logger.info("Google CSE query={} results={}", candidate, results.size());
      if (!results.isEmpty()) {
        queryUsed = candidate;
        usedLocale = true;
        break;
      }
      if (hasLocale) {
        results = googleSearchService.search(candidate, apiKey, searchEngineId, maxPerRequest, null, null);
        logger.info("Google CSE query={} locale=none results={}", candidate, results.size());
        if (!results.isEmpty()) {
          queryUsed = candidate;
          usedLocale = false;
          break;
        }
      }
    }
    if (results.isEmpty() || queryUsed == null) {
      logger.info("Google CSE returned no results queries={}", queries);
      return Optional.empty();
    }
    logger.info("Google CSE selected query={} localeUsed={}", queryUsed, usedLocale);
    Set<String> allowedDomains = parseAllowedDomains(settings.getAllowedDomains());
    boolean allowAll = allowedDomains.contains("*");
    logger.info("Search evidence allowAll={} allowedDomains={}", allowAll, allowAll ? "*" : allowedDomains);
    List<EvidencePage> pages = new ArrayList<>();
    for (SearchResult result : results) {
      String url = result.url();
      if (url == null || url.isBlank()) {
        continue;
      }
      if (skipUrl != null && !skipUrl.isBlank() && skipUrl.equalsIgnoreCase(url)) {
        logger.info("Search evidence skip url reason=SKIP_URL url={}", url);
        continue;
      }
      if (!allowAll && !isAllowed(url, allowedDomains)) {
        logger.info("Search evidence skip url reason=DOMAIN_BLOCKED url={}", url);
        continue;
      }
      EvidencePage page = buildEvidencePage(result);
      if (page != null) {
        pages.add(page);
      }
      if (pages.size() >= MAX_EVIDENCE_PAGES) {
        break;
      }
    }
    if (pages.isEmpty()) {
      logger.info("Search evidence captured none query={}", queryUsed);
      return Optional.empty();
    }
    logger.info("Google CSE evidence captured query={} pages={} urls={}",
        queryUsed,
        pages.size(),
        pages.stream().map(EvidencePage::url).toList());
    return Optional.of(new SearchEvidence(queryUsed, pages));
  }

  private EvidencePage buildEvidencePage(SearchResult result) {
    String url = result.url();
    Optional<String> html = htmlFetchService.fetchHtml(url);
    String pageText = "";
    String linkHints = "";
    if (html.isPresent()) {
      org.jsoup.nodes.Document document = Jsoup.parse(html.get(), url);
      String text = document.text();
      if (text != null) {
        pageText = text.length() > MAX_PAGE_TEXT_CHARS ? text.substring(0, MAX_PAGE_TEXT_CHARS) : text;
      }
      linkHints = extractLinkHints(document);
    }
    logger.info("Search evidence page url={} htmlLoaded={} textChars={} linkHintsCount={}",
        url,
        html.isPresent(),
        pageText.length(),
        linkHints.isBlank() ? 0 : linkHints.split("\\n").length);
    return new EvidencePage(url, result.title(), result.snippet(), pageText, linkHints);
  }

  private List<String> buildQueryVariants(Journalist journalist, String articleTitle) {
    String name = safeQuoted(journalist.getFullName());
    String publication = safeQuoted(journalist.getPublicationName());
    String domain = normalizeDomain(journalist.getPublicationDomain());
    String titleTerm = safeTerm(articleTitle);
    Set<String> queries = new java.util.LinkedHashSet<>();
    queries.add(joinTerms(name, publication, "journalist profile"));
    queries.add(joinTerms(name, publication, "author profile"));
    if (!titleTerm.isBlank()) {
      queries.add(joinTerms(name, publication, titleTerm));
    }
    queries.add(joinTerms(name, publication));
    if (!domain.isBlank()) {
      queries.add(joinTerms(name, "site:" + domain));
    }
    queries.add(joinTerms(name, "site:linkedin.com"));
    queries.add(joinTerms(name, "site:muckrack.com"));
    queries.add(joinTerms(name, "site:goskribe.com"));
    queries.add(joinTerms(name, "site:twitter.com"));
    queries.removeIf(String::isBlank);
    return new ArrayList<>(queries);
  }

  private String safeQuoted(String value) {
    if (value == null) {
      return "";
    }
    String cleaned = value.replace("\"", "").trim();
    if (cleaned.isBlank()) {
      return "";
    }
    return "\"" + cleaned + "\"";
  }

  private String safeTerm(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\"", "").trim();
  }

  private String joinTerms(String... terms) {
    List<String> values = new ArrayList<>();
    for (String term : terms) {
      if (term == null) {
        continue;
      }
      String trimmed = term.trim();
      if (!trimmed.isBlank()) {
        values.add(trimmed);
      }
    }
    return String.join(" ", values);
  }

  private String normalizeDomain(String domain) {
    if (domain == null || domain.isBlank()) {
      return "";
    }
    String trimmed = domain.trim().toLowerCase(Locale.ROOT);
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      try {
        String host = URI.create(trimmed).getHost();
        return host == null ? "" : host;
      } catch (Exception ex) {
        return "";
      }
    }
    int slashIndex = trimmed.indexOf('/');
    return slashIndex > 0 ? trimmed.substring(0, slashIndex) : trimmed;
  }

  private String extractArticleTitle(List<Article> recentArticles) {
    if (recentArticles == null) {
      return "";
    }
    for (Article article : recentArticles) {
      if (article == null || article.getTitle() == null || article.getTitle().isBlank()) {
        continue;
      }
      String title = article.getTitle().trim();
      if (title.length() > 80) {
        title = title.substring(0, 80).trim();
      }
      return title;
    }
    return "";
  }

  private LocalePreference resolveLocaleFromProfile() {
    try {
      var user = currentUserService.requireCurrentUser();
      UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
      if (profile == null) {
        return new LocalePreference(null, null, "none");
      }
      String lang = first(profile.getPreferredLangs());
      String country = first(profile.getPreferredCountries());
      return new LocalePreference(country, lang, "profile");
    } catch (Exception ex) {
      return new LocalePreference(null, null, "none");
    }
  }

  private String first(String[] values) {
    if (values == null || values.length == 0) {
      return null;
    }
    String first = values[0];
    return first == null || first.isBlank() ? null : first.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isAllowed(String url, Set<String> allowedDomains) {
    try {
      String host = URI.create(url).getHost();
      if (host == null) {
        return false;
      }
      String normalized = host.toLowerCase(Locale.ROOT);
      for (String domain : allowedDomains) {
        if (domain.isBlank()) {
          continue;
        }
        String trimmed = domain.toLowerCase(Locale.ROOT);
        if (normalized.equals(trimmed) || normalized.endsWith("." + trimmed)) {
          return true;
        }
      }
    } catch (Exception ex) {
      return false;
    }
    return false;
  }

  private Set<String> parseAllowedDomains(String value) {
    String source = value;
    if (source == null || source.isBlank()) {
      source = searchProviderProperties.getGoogle().getAllowedDomains();
    }
    if (source == null || source.isBlank()) {
      return Set.of("*");
    }
    String normalized = source.trim();
    if ("*".equals(normalized)) {
      return Set.of("*");
    }
    Set<String> domains = new HashSet<>();
    for (String token : normalized.split("[,\\n]")) {
      String cleaned = token.trim();
      if (cleaned.isBlank()) {
        continue;
      }
      if ("*".equals(cleaned)) {
        return Set.of("*");
      }
      domains.add(cleaned);
    }
    return domains.isEmpty() ? Set.of("*") : domains;
  }

  private String fallback(String primary, String secondary) {
    if (primary != null && !primary.isBlank()) {
      return primary;
    }
    return secondary;
  }

  private String extractLinkHints(org.jsoup.nodes.Document document) {
    if (document == null) {
      return "";
    }
    List<String> hints = new ArrayList<>();
    for (org.jsoup.nodes.Element link : document.select("a[href]")) {
      String href = link.attr("href");
      if (href == null || href.isBlank()) {
        continue;
      }
      String normalized = href.toLowerCase(Locale.ROOT);
      if (!(normalized.contains("linkedin.com") || normalized.contains("twitter.com") || normalized.contains("x.com")
          || normalized.contains("facebook.com") || normalized.contains("instagram.com"))) {
        continue;
      }
      hints.add(href.trim());
      if (hints.size() >= MAX_LINK_HINTS) {
        break;
      }
    }
    return String.join("\n", hints);
  }

  public record SearchEvidence(String query, List<EvidencePage> pages) {
    public String summary() {
      StringBuilder builder = new StringBuilder();
      builder.append("Query: ").append(query).append("\n");
      for (EvidencePage page : pages) {
        builder.append("- URL: ").append(page.url()).append("\n");
        if (page.title() != null && !page.title().isBlank()) {
          builder.append("  Title: ").append(page.title()).append("\n");
        }
        if (page.snippet() != null && !page.snippet().isBlank()) {
          builder.append("  Snippet: ").append(page.snippet()).append("\n");
        }
        if (page.pageText() != null && !page.pageText().isBlank()) {
          builder.append("  Page text: ").append(page.pageText()).append("\n");
        }
        if (page.linkHints() != null && !page.linkHints().isBlank()) {
          builder.append("  Link hints: ").append(page.linkHints()).append("\n");
        }
      }
      return builder.toString().trim();
    }
  }

  public record EvidencePage(String url, String title, String snippet, String pageText, String linkHints) {
  }

  private record LocalePreference(String country, String lang, String source) {
  }
}
