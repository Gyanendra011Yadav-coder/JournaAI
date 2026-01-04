package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ContactSourceType;
import ai.journa.prcontrol.domain.ContactVisibility;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.repository.JournalistContactRepository;
import ai.journa.prcontrol.service.llm.LlmClientService;
import ai.journa.prcontrol.service.llm.LlmRequest;
import ai.journa.prcontrol.service.llm.LlmResponse;
import ai.journa.prcontrol.service.llm.PromptFileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JournalistEnrichmentService {
  private static final Logger logger = LoggerFactory.getLogger(JournalistEnrichmentService.class);
  private static final int MAX_ARTICLE_SNIPPETS = 8;
  private static final int MAX_PAGE_TEXT_CHARS = 2500;
  private static final int MAX_LINK_HINTS = 6;
  private static final int MAX_DISCOVERY_ARTICLES = 3;
  private static final int MAX_CONTACT_EVIDENCE_CHARS = 12000;
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
  private static final Pattern TEL_PATTERN = Pattern.compile("(?i)tel:\\s*([+\\d][\\d\\s().-]{5,})");
  private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d[\\d\\s().-]{6,}\\d)");

  private final LlmClientService llmClientService;
  private final PromptFileService promptFileService;
  private final ObjectMapper objectMapper;
  private final HtmlFetchService htmlFetchService;
  private final JournalistEnrichmentReviewService reviewService;
  private final SearchEvidenceService searchEvidenceService;
  private final JournalistContactRepository contactRepository;

  public JournalistEnrichmentService(LlmClientService llmClientService,
                                     PromptFileService promptFileService,
                                     ObjectMapper objectMapper,
                                     HtmlFetchService htmlFetchService,
                                     JournalistEnrichmentReviewService reviewService,
                                     SearchEvidenceService searchEvidenceService,
                                     JournalistContactRepository contactRepository) {
    this.llmClientService = llmClientService;
    this.promptFileService = promptFileService;
    this.objectMapper = objectMapper;
    this.htmlFetchService = htmlFetchService;
    this.reviewService = reviewService;
    this.searchEvidenceService = searchEvidenceService;
    this.contactRepository = contactRepository;
  }

  public EnrichmentOutcome enrich(Journalist journalist, List<Article> recentArticles) {
    if (journalist == null || journalist.getId() == null) {
      return EnrichmentOutcome.failed("Missing journalist");
    }
    logger.info("Journalist enrichment start journalistId={} name={}",
        journalist.getId(),
        journalist.getFullName());
    String currentJson = writeJson(journalist);
    String authorPageUrl = journalist.getAuthorPageUrl();
    AuthorPageDiscovery discovery = discoverAuthorPage(journalist, recentArticles);
    if ((authorPageUrl == null || authorPageUrl.isBlank()) && discovery != null) {
      authorPageUrl = discovery.url();
    }
    PageSignals pageSignals = fetchPageSignals(authorPageUrl);
    String articleSnippets = buildArticleSnippets(recentArticles);
    String searchEvidence = searchEvidenceService.buildEvidence(journalist, recentArticles, authorPageUrl)
        .map(SearchEvidenceService.SearchEvidence::summary)
        .orElse("");

    String userPrompt = promptFileService.renderJournalistUserPrompt(Map.of(
        "CURRENT_JOURNALIST_JSON", currentJson,
        "AUTHOR_PAGE_URL", safe(authorPageUrl),
        "AUTHOR_PAGE_TEXT", safe(pageSignals.text()),
        "AUTHOR_PAGE_LINKS", safe(pageSignals.links()),
        "AUTHOR_PAGE_DISCOVERY", discovery != null ? discovery.summary() : "",
        "SEARCH_EVIDENCE", safe(searchEvidence),
        "RECENT_ARTICLES", safe(articleSnippets)
    ));
    try {
      JsonNode root = fetchEnrichmentPayload(journalist, userPrompt);
      JsonNode proposed = root.path("proposed_profile");
      if (proposed.isMissingNode() || proposed.isNull()) {
        logger.warn("Journalist enrichment failed journalistId={} reason=EMPTY_PROFILE", journalist.getId());
        return EnrichmentOutcome.failed("Empty profile");
      }
      ensureBioSummary(root);
      applyEvidenceOverrides(root, authorPageUrl, pageSignals, searchEvidence);
      persistContacts(journalist, pageSignals, searchEvidence);
      boolean queued = reviewService.createReview(journalist, proposed).isPresent();
      if (queued) {
        logger.info("Journalist enrichment queued journalistId={}", journalist.getId());
        return EnrichmentOutcome.queuedOutcome();
      }
      logger.info("Journalist enrichment no changes journalistId={}", journalist.getId());
      return EnrichmentOutcome.noChanges();
    } catch (Exception ex) {
      logger.warn("Journalist enrichment failed journalistId={} reason={}", journalist.getId(), ex.getMessage());
      return EnrichmentOutcome.failed(ex.getMessage());
    }
  }

  public record EnrichmentOutcome(boolean queued, boolean changed, String message) {
    public static EnrichmentOutcome queuedOutcome() {
      return new EnrichmentOutcome(true, true, "Queued for review");
    }

    public static EnrichmentOutcome noChanges() {
      return new EnrichmentOutcome(false, false, "No changes");
    }

    public static EnrichmentOutcome failed(String message) {
      return new EnrichmentOutcome(false, false, message);
    }
  }

  private String buildArticleSnippets(List<Article> articles) {
    if (articles == null || articles.isEmpty()) {
      return "";
    }
    List<String> lines = new ArrayList<>();
    int count = 0;
    for (Article article : articles) {
      if (article == null || article.getTitle() == null || article.getTitle().isBlank()) {
        continue;
      }
      String url = safe(article.getUrl());
      lines.add("- " + article.getTitle().trim() + (url.isBlank() ? "" : " | " + url));
      count++;
      if (count >= MAX_ARTICLE_SNIPPETS) {
        break;
      }
    }
    return String.join("\n", lines);
  }

  private void ensureBioSummary(JsonNode root) {
    if (root == null || !root.isObject()) {
      return;
    }
    JsonNode proposed = root.path("proposed_profile");
    if (!proposed.isObject()) {
      return;
    }
    String current = text(proposed, "bio_summary");
    if (current != null && !current.isBlank()) {
      return;
    }
    String summary = buildFallbackSummary(proposed);
    if (summary == null || summary.isBlank()) {
      return;
    }
    ((ObjectNode) proposed).put("bio_summary", summary);
  }

  private void applyEvidenceOverrides(JsonNode root, String authorPageUrl, PageSignals pageSignals, String searchEvidence) {
    if (root == null || !root.isObject()) {
      return;
    }
    JsonNode proposed = root.path("proposed_profile");
    if (!proposed.isObject()) {
      return;
    }
    ObjectNode mutable = (ObjectNode) proposed;
    ObjectNode links = proposed.path("public_links").isObject()
        ? (ObjectNode) proposed.path("public_links")
        : mutable.putObject("public_links");
    if (isBlank(text(links, "author_page")) && hasText(authorPageUrl)) {
      links.put("author_page", authorPageUrl);
    }
    EvidenceLinks evidenceLinks = extractEvidenceLinks(pageSignals, searchEvidence, text(proposed, "full_name"));
    if (isBlank(text(links, "linkedin")) && evidenceLinks.linkedin() != null) {
      links.put("linkedin", evidenceLinks.linkedin());
    }
    if (isBlank(text(links, "twitter")) && evidenceLinks.twitter() != null) {
      links.put("twitter", evidenceLinks.twitter());
    }
    if (isBlank(text(proposed, "journey_summary"))) {
      String bio = text(proposed, "bio_summary");
      if (bio != null && !bio.isBlank()) {
        mutable.put("journey_summary", bio);
      }
    }
  }

  private EvidenceLinks extractEvidenceLinks(PageSignals pageSignals, String searchEvidence, String fullName) {
    String combined = buildEvidenceText(pageSignals, searchEvidence);
    Set<String> linkedInUrls = new LinkedHashSet<>();
    Set<String> twitterUrls = new LinkedHashSet<>();
    Matcher matcher = URL_PATTERN.matcher(combined);
    while (matcher.find()) {
      String raw = trimUrl(matcher.group());
      if (raw == null || raw.isBlank()) {
        continue;
      }
      String normalized = raw.toLowerCase(Locale.ROOT);
      if (normalized.contains("linkedin.com")) {
        linkedInUrls.add(raw);
      } else if (normalized.contains("twitter.com") || normalized.contains("x.com")) {
        twitterUrls.add(raw);
      }
    }
    String linkedin = selectBestLinkedIn(linkedInUrls, fullName);
    String twitter = selectBestTwitter(twitterUrls, fullName);
    return new EvidenceLinks(linkedin, twitter);
  }

  private String selectBestLinkedIn(Set<String> urls, String fullName) {
    if (urls == null || urls.isEmpty()) {
      return null;
    }
    String nameSlug = slugify(fullName);
    for (String url : urls) {
      String lower = url.toLowerCase(Locale.ROOT);
      if (lower.contains("/in/") || lower.contains("/pub/")) {
        return url;
      }
    }
    if (nameSlug != null && !nameSlug.isBlank()) {
      for (String url : urls) {
        if (url.toLowerCase(Locale.ROOT).contains(nameSlug)) {
          return url;
        }
      }
    }
    for (String url : urls) {
      if (!url.toLowerCase(Locale.ROOT).contains("/company/")) {
        return url;
      }
    }
    return urls.iterator().next();
  }

  private String selectBestTwitter(Set<String> urls, String fullName) {
    if (urls == null || urls.isEmpty()) {
      return null;
    }
    String nameSlug = slugify(fullName);
    for (String url : urls) {
      String normalized = url.toLowerCase(Locale.ROOT);
      if (normalized.contains("/share") || normalized.contains("/intent")) {
        continue;
      }
      if (nameSlug != null && !nameSlug.isBlank() && normalized.contains(nameSlug)) {
        return url;
      }
    }
    for (String url : urls) {
      String normalized = url.toLowerCase(Locale.ROOT);
      if (normalized.contains("/share") || normalized.contains("/intent")) {
        continue;
      }
      return url;
    }
    return urls.iterator().next();
  }

  private String trimUrl(String url) {
    if (url == null) {
      return null;
    }
    String trimmed = url.trim();
    while (!trimmed.isBlank() && (trimmed.endsWith(",") || trimmed.endsWith(".") || trimmed.endsWith(")") || trimmed.endsWith("]"))) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  private void persistContacts(Journalist journalist, PageSignals pageSignals, String searchEvidence) {
    if (journalist == null || journalist.getId() == null) {
      return;
    }
    ContactCandidates candidates = extractContactCandidates(journalist, pageSignals, searchEvidence);
    if (candidates.emails().isEmpty() && candidates.phones().isEmpty()) {
      return;
    }
    List<JournalistContact> existing = contactRepository.findByJournalistId(journalist.getId());
    Set<String> existingEmails = new LinkedHashSet<>();
    Set<String> existingPhones = new LinkedHashSet<>();
    for (JournalistContact contact : existing) {
      if (contact.getEmail() != null) {
        existingEmails.add(contact.getEmail().trim().toLowerCase(Locale.ROOT));
      }
      if (contact.getPhone() != null) {
        existingPhones.add(normalizePhone(contact.getPhone()));
      }
    }
    List<JournalistContact> created = new ArrayList<>();
    for (String email : candidates.emails()) {
      String normalized = email.toLowerCase(Locale.ROOT);
      if (existingEmails.contains(normalized)) {
        continue;
      }
      JournalistContact contact = new JournalistContact();
      contact.setJournalist(journalist);
      contact.setEmail(email);
      contact.setVisibility(ContactVisibility.ADMIN_ONLY);
      contact.setSourceType(ContactSourceType.PUBLIC_BIO);
      created.add(contact);
      existingEmails.add(normalized);
    }
    for (String phone : candidates.phones()) {
      String normalized = normalizePhone(phone);
      if (normalized.isBlank() || existingPhones.contains(normalized)) {
        continue;
      }
      JournalistContact contact = new JournalistContact();
      contact.setJournalist(journalist);
      contact.setPhone(normalized);
      contact.setVisibility(ContactVisibility.ADMIN_ONLY);
      contact.setSourceType(ContactSourceType.PUBLIC_BIO);
      created.add(contact);
      existingPhones.add(normalized);
    }
    if (!created.isEmpty()) {
      contactRepository.saveAll(created);
      logger.info("Journalist contact extracted journalistId={} emails={} phones={}",
          journalist.getId(),
          candidates.emails().size(),
          candidates.phones().size());
    }
  }

  private ContactCandidates extractContactCandidates(Journalist journalist, PageSignals pageSignals, String searchEvidence) {
    String combined = buildEvidenceText(pageSignals, searchEvidence);
    Set<String> emails = new LinkedHashSet<>();
    Set<String> phones = new LinkedHashSet<>();
    Matcher emailMatcher = EMAIL_PATTERN.matcher(combined);
    while (emailMatcher.find()) {
      String email = trimPunctuation(emailMatcher.group());
      if (email != null && !email.isBlank() && isLikelyJournalistEmail(email, journalist)) {
        emails.add(email.toLowerCase(Locale.ROOT));
      }
    }
    String sanitized = stripUrls(combined);
    Matcher telMatcher = TEL_PATTERN.matcher(sanitized);
    while (telMatcher.find()) {
      String phone = normalizePhone(telMatcher.group(1));
      if (!phone.isBlank()) {
        phones.add(phone);
      }
    }
    String[] lines = sanitized.split("\\R");
    for (String line : lines) {
      String lower = line.toLowerCase(Locale.ROOT);
      boolean hasKeyword = hasPhoneKeyword(lower);
      Matcher phoneMatcher = PHONE_PATTERN.matcher(line);
      while (phoneMatcher.find()) {
        String raw = phoneMatcher.group();
        if (!isLikelyPhone(raw, hasKeyword)) {
          continue;
        }
        String phone = normalizePhone(raw);
        if (!phone.isBlank()) {
          phones.add(phone);
        }
      }
    }
    return new ContactCandidates(emails, phones);
  }

  private String buildEvidenceText(PageSignals pageSignals, String searchEvidence) {
    StringBuilder builder = new StringBuilder();
    if (pageSignals != null) {
      appendEvidence(builder, pageSignals.text());
      appendEvidence(builder, pageSignals.links());
    }
    appendEvidence(builder, searchEvidence);
    String combined = builder.toString();
    if (combined.length() <= MAX_CONTACT_EVIDENCE_CHARS) {
      return combined;
    }
    return combined.substring(0, MAX_CONTACT_EVIDENCE_CHARS);
  }

  private void appendEvidence(StringBuilder builder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append('\n');
    }
    builder.append(value);
  }

  private String trimPunctuation(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    while (!trimmed.isBlank() && (trimmed.endsWith(",") || trimmed.endsWith(".") || trimmed.endsWith(";") || trimmed.endsWith(")"))) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  private String stripUrls(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return URL_PATTERN.matcher(value).replaceAll(" ");
  }

  private String normalizePhone(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    boolean hasPlus = trimmed.startsWith("+") || trimmed.startsWith("00");
    String digits = trimmed.replaceAll("[^0-9]", "");
    if (trimmed.startsWith("00") && digits.length() > 2) {
      digits = digits.substring(2);
    }
    if (digits.length() < 8 || digits.length() > 15) {
      return "";
    }
    return hasPlus ? "+" + digits : digits;
  }

  private boolean hasPhoneKeyword(String line) {
    if (line == null) {
      return false;
    }
    return line.contains("phone") || line.contains("mobile") || line.contains("call")
        || line.contains("contact") || line.contains("tel") || line.contains("whatsapp");
  }

  private boolean isLikelyPhone(String raw, boolean hasKeyword) {
    if (raw == null || raw.isBlank()) {
      return false;
    }
    String digits = raw.replaceAll("[^0-9]", "");
    int length = digits.length();
    boolean hasSeparator = raw.matches(".*[\\s().-].*");
    boolean hasPlus = raw.trim().startsWith("+") || raw.trim().startsWith("00");
    if (length >= 10) {
      return true;
    }
    if ((hasSeparator || hasPlus) && length >= 8) {
      return true;
    }
    return hasKeyword && length >= 7;
  }

  private boolean isLikelyJournalistEmail(String email, Journalist journalist) {
    if (email == null || journalist == null) {
      return false;
    }
    String lower = email.toLowerCase(Locale.ROOT);
    int atIndex = lower.indexOf('@');
    if (atIndex <= 0) {
      return false;
    }
    String local = lower.substring(0, atIndex);
    String domain = lower.substring(atIndex + 1);
    String publicationDomain = normalizeDomain(journalist.getPublicationDomain());
    if (publicationDomain != null && !publicationDomain.isBlank() && domain.endsWith(publicationDomain)) {
      return true;
    }
    String fullName = journalist.getFullName() == null ? "" : journalist.getFullName().toLowerCase(Locale.ROOT);
    for (String part : fullName.split("\\s+")) {
      String token = part.replaceAll("[^a-z0-9]", "");
      if (token.length() < 3) {
        continue;
      }
      if (local.contains(token)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeDomain(String domain) {
    if (domain == null || domain.isBlank()) {
      return "";
    }
    String trimmed = domain.trim().toLowerCase(Locale.ROOT);
    try {
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        String host = URI.create(trimmed).getHost();
        if (host == null) {
          return "";
        }
        return host.startsWith("www.") ? host.substring(4) : host;
      }
    } catch (Exception ignored) {
    }
    int slashIndex = trimmed.indexOf('/');
    String host = slashIndex > 0 ? trimmed.substring(0, slashIndex) : trimmed;
    return host.startsWith("www.") ? host.substring(4) : host;
  }

  private String buildFallbackSummary(JsonNode proposed) {
    String name = text(proposed, "full_name");
    String designation = text(proposed, "designation");
    String publication = text(proposed, "publication_name");
    JsonNode location = proposed.path("location");
    String city = text(location, "city");
    String country = text(location, "country");
    List<String> beats = extractBeats(proposed.path("beats"));

    List<String> lines = new ArrayList<>();
    if (hasText(name) && hasText(designation) && hasText(publication)) {
      lines.add(name + " is a " + designation + " at " + publication + ".");
    } else if (hasText(designation) && hasText(publication)) {
      lines.add("A " + designation + " at " + publication + ".");
    } else if (hasText(name) && hasText(publication)) {
      lines.add(name + " writes for " + publication + ".");
    }
    if (!beats.isEmpty()) {
      lines.add("Covers " + String.join(", ", beats) + ".");
    }
    if (hasText(city) || hasText(country)) {
      String locationText = hasText(city) && hasText(country)
          ? city + ", " + country
          : hasText(city) ? city : country;
      lines.add("Based in " + locationText + ".");
    }
    if (lines.isEmpty()) {
      return null;
    }
    return String.join(" ", lines);
  }

  private List<String> extractBeats(JsonNode beatsNode) {
    List<String> beats = new ArrayList<>();
    if (beatsNode == null || beatsNode.isMissingNode() || beatsNode.isNull()) {
      return beats;
    }
    if (beatsNode.isArray()) {
      for (JsonNode node : beatsNode) {
        String beat = node.asText(null);
        if (beat != null && !beat.isBlank()) {
          beats.add(beat.trim());
        }
      }
    } else {
      String beat = beatsNode.asText(null);
      if (beat != null && !beat.isBlank()) {
        beats.add(beat.trim());
      }
    }
    return beats;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record EvidenceLinks(String linkedin, String twitter) {
  }

  private record ContactCandidates(Set<String> emails, Set<String> phones) {
  }

  private PageSignals fetchPageSignals(String url) {
    if (url == null || url.isBlank()) {
      return new PageSignals("", "");
    }
    Optional<String> html = htmlFetchService.fetchHtml(url);
    if (html.isEmpty()) {
      return new PageSignals("", "");
    }
    org.jsoup.nodes.Document document = Jsoup.parse(html.get());
    String text = document.text();
    if (text.length() <= MAX_PAGE_TEXT_CHARS) {
      return new PageSignals(text, extractLinkHints(document));
    }
    return new PageSignals(text.substring(0, MAX_PAGE_TEXT_CHARS), extractLinkHints(document));
  }

  private AuthorPageDiscovery discoverAuthorPage(Journalist journalist, List<Article> recentArticles) {
    if (journalist == null || journalist.getFullName() == null || journalist.getFullName().isBlank()) {
      return null;
    }
    if (recentArticles == null || recentArticles.isEmpty()) {
      return null;
    }
    String name = journalist.getFullName().trim();
    String normalizedName = normalizeKey(name);
    int attempts = 0;
    for (Article article : recentArticles) {
      if (article == null || article.getUrl() == null || article.getUrl().isBlank()) {
        continue;
      }
      attempts++;
      Optional<String> html = htmlFetchService.fetchHtml(article.getUrl());
      if (html.isEmpty()) {
        continue;
      }
      org.jsoup.nodes.Document document = Jsoup.parse(html.get(), article.getUrl());
      String candidate = findAuthorLink(document, normalizedName, name);
      if (candidate != null && !candidate.isBlank()) {
        String summary = "Discovered from article: " + article.getUrl();
        return new AuthorPageDiscovery(candidate, summary);
      }
      if (attempts >= MAX_DISCOVERY_ARTICLES) {
        break;
      }
    }
    return null;
  }

  private String findAuthorLink(org.jsoup.nodes.Document document, String normalizedName, String rawName) {
    if (document == null) {
      return null;
    }
    for (org.jsoup.nodes.Element element : document.select("a[rel=author], link[rel=author]")) {
      String url = element.hasAttr("href") ? element.absUrl("href") : element.absUrl("content");
      String text = element.text();
      if (matchesName(normalizedName, rawName, text, url)) {
        return url;
      }
    }
    for (org.jsoup.nodes.Element element : document.select("[itemprop=author], [class*=author], [class*=byline]")) {
      for (org.jsoup.nodes.Element anchor : element.select("a[href]")) {
        String url = anchor.absUrl("href");
        if (url.isBlank()) {
          continue;
        }
        String text = anchor.text();
        String aria = anchor.attr("aria-label");
        if (matchesName(normalizedName, rawName, text, aria) || matchesName(normalizedName, rawName, text, url)) {
          return url;
        }
      }
    }
    String jsonLdUrl = findAuthorUrlFromJsonLd(document, normalizedName, rawName);
    if (jsonLdUrl != null && !jsonLdUrl.isBlank()) {
      return jsonLdUrl;
    }
    for (org.jsoup.nodes.Element anchor : document.select("a[href*=/author/], a[href*=/authors/], a[href*=/profile/], a[href*=/writer/], a[href*=/reporter/], a[href*=/journalist/]")) {
      String url = anchor.absUrl("href");
      if (url.isBlank()) {
        continue;
      }
      String text = anchor.text();
      String aria = anchor.attr("aria-label");
      if (matchesName(normalizedName, rawName, text, aria) || matchesName(normalizedName, rawName, text, url)) {
        return url;
      }
    }
    return null;
  }

  private String findAuthorUrlFromJsonLd(org.jsoup.nodes.Document document, String normalizedName, String rawName) {
    for (org.jsoup.nodes.Element script : document.select("script[type=application/ld+json]")) {
      try {
        JsonNode node = objectMapper.readTree(script.html());
        String url = extractAuthorUrl(node, normalizedName, rawName);
        if (url != null && !url.isBlank()) {
          return url;
        }
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private String extractAuthorUrl(JsonNode node, String normalizedName, String rawName) {
    if (node == null) {
      return null;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        String url = extractAuthorUrl(child, normalizedName, rawName);
        if (url != null) {
          return url;
        }
      }
      return null;
    }
    JsonNode author = node.get("author");
    if (author != null) {
      if (author.isArray()) {
        for (JsonNode item : author) {
          String url = extractAuthorUrl(item, normalizedName, rawName);
          if (url != null) {
            return url;
          }
        }
      } else {
        String url = extractAuthorUrl(author, normalizedName, rawName);
        if (url != null) {
          return url;
        }
      }
    }
    if (node.isObject()) {
      String name = node.path("name").asText(null);
      if (name != null) {
        if (matchesName(normalizedName, rawName, name)) {
          String url = node.path("url").asText(null);
          if (url == null || url.isBlank()) {
            JsonNode sameAs = node.path("sameAs");
            if (sameAs.isTextual()) {
              url = sameAs.asText();
            } else if (sameAs.isArray() && sameAs.size() > 0) {
              url = sameAs.get(0).asText();
            }
          }
          if (url != null && !url.isBlank()) {
            return url;
          }
        }
      }
    }
    return null;
  }

  private boolean matchesName(String normalizedName, String rawName, String... values) {
    if (normalizedName == null || normalizedName.isBlank()) {
      return false;
    }
    String slug = slugify(rawName);
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      String normalizedValue = normalizeKey(value);
      if (!normalizedValue.isBlank()) {
        if (normalizedValue.contains(normalizedName) || normalizedName.contains(normalizedValue)) {
          return true;
        }
      }
      if (slug != null && !slug.isBlank()) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains(slug)) {
          return true;
        }
      }
    }
    return false;
  }

  private String normalizeKey(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private String slugify(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    return normalized.replaceAll("(^-+|-+$)", "");
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
      String normalized = href.toLowerCase();
      if (!(normalized.contains("linkedin.com") || normalized.contains("twitter.com") || normalized.contains("x.com"))) {
        continue;
      }
      hints.add(href.trim());
      if (hints.size() >= MAX_LINK_HINTS) {
        break;
      }
    }
    return String.join("\n", hints);
  }

  private String text(JsonNode node, String field) {
    if (node == null) {
      return null;
    }
    String value = node.path(field).asText(null);
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private JsonNode fetchEnrichmentPayload(Journalist journalist, String userPrompt) throws Exception {
    LlmRequest request = new LlmRequest(
        promptFileService.getJournalistSystemPrompt(),
        userPrompt,
        0.2,
        1200
    );
    Optional<LlmResponse> response = llmClientService.generateValidated(request, null, this::isValidJsonPayload);
    if (response.isPresent()) {
      ParseAttempt parsed = parseJsonPayload(response.get().content());
      if (parsed.node().isPresent() && hasRequiredFields(parsed.node().get())) {
        return parsed.node().get();
      }
    }
    response = llmClientService.generate(request, null);
    if (response.isEmpty()) {
      logger.warn("Journalist enrichment skipped journalistId={} reason=NO_LLM_RESPONSE", journalist.getId());
      throw new IllegalStateException("No LLM response");
    }
    ParseAttempt parsed = parseJsonPayload(response.get().content());
    if (parsed.node().isPresent()) {
      if (!hasRequiredFields(parsed.node().get())) {
        logger.warn("Journalist enrichment retry journalistId={} reason=SCHEMA_INCOMPLETE", journalist.getId());
      } else {
        return parsed.node().get();
      }
    }
    logger.warn("Journalist enrichment invalid JSON journalistId={} error={} rawResponse={} sanitizedResponse={}",
        journalist.getId(),
        parsed.errorMessage(),
        truncate(response.get().content(), 4000),
        truncate(parsed.sanitized(), 4000));
    logger.warn("Journalist enrichment retry journalistId={} reason=INVALID_JSON", journalist.getId());
    LlmRequest repair = new LlmRequest(
        promptFileService.getJournalistSystemPrompt(),
        buildRepairPrompt(response.get().content()),
        0.0,
        700
    );
    Optional<LlmResponse> repaired = llmClientService.generate(repair, null);
    if (repaired.isEmpty()) {
      throw new IllegalStateException("LLM repair failed");
    }
    ParseAttempt repairedParsed = parseJsonPayload(repaired.get().content());
    if (repairedParsed.node().isPresent() && hasRequiredFields(repairedParsed.node().get())) {
      return repairedParsed.node().get();
    }
    logger.warn("Journalist enrichment invalid JSON after repair journalistId={} error={} rawResponse={} repairedResponse={} sanitizedResponse={}",
        journalist.getId(),
        repairedParsed.errorMessage(),
        truncate(response.get().content(), 4000),
        truncate(repaired.get().content(), 4000),
        truncate(repairedParsed.sanitized(), 4000));
    throw new IllegalStateException("Invalid JSON after repair");
  }

  private boolean isValidJsonPayload(String content) {
    ParseAttempt parsed = parseJsonPayload(content);
    return parsed.node().isPresent() && hasRequiredFields(parsed.node().get());
  }

  private boolean hasRequiredFields(JsonNode root) {
    if (root == null || !root.isObject()) {
      return false;
    }
    if (!root.has("proposed_profile") || !root.has("confidence")
        || !root.has("evidence") || !root.has("needs_admin_approval")) {
      return false;
    }
    JsonNode profile = root.path("proposed_profile");
    if (!profile.isObject()) {
      return false;
    }
    if (!profile.has("full_name") || !profile.has("publication_name") || !profile.has("publication_domain")
        || !profile.has("designation") || !profile.has("beats") || !profile.has("location")
        || !profile.has("public_links") || !profile.has("bio_summary")) {
      return false;
    }
    JsonNode location = profile.path("location");
    if (!location.isObject() || !location.has("country") || !location.has("city")) {
      return false;
    }
    JsonNode links = profile.path("public_links");
    return links.isObject() && links.has("author_page") && links.has("twitter") && links.has("linkedin");
  }

  private ParseAttempt parseJsonPayload(String content) {
    if (content == null || content.isBlank()) {
      return ParseAttempt.failure("Empty LLM response", null);
    }
    try {
      return ParseAttempt.success(objectMapper.readTree(content));
    } catch (Exception ex) {
      String sanitized = extractJson(content);
      if (sanitized == null || sanitized.isBlank()) {
        return ParseAttempt.failure(ex.getMessage(), null);
      }
      try {
        return ParseAttempt.success(objectMapper.readTree(sanitized));
      } catch (Exception ex2) {
        Optional<JsonNode> repaired = attemptAutoCloseJson(sanitized);
        if (repaired.isPresent()) {
          return ParseAttempt.success(repaired.get());
        }
        return ParseAttempt.failure(ex2.getMessage(), sanitized);
      }
    }
  }

  private String buildRepairPrompt(String content) {
    return "The previous response was invalid or incomplete JSON. Re-emit a valid JSON object only, " +
        "matching the schema exactly. Do not include markdown or commentary. " +
        "Include every key in the schema; use null or empty arrays when unknown. " +
        "Here is the invalid response:\n" + safe(content);
  }

  private String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    if (trimmed.length() <= max) {
      return trimmed;
    }
    return trimmed.substring(0, max) + "...(truncated)";
  }

  private String extractJson(String content) {
    if (content == null) {
      return null;
    }
    String trimmed = content.trim();
    if (trimmed.startsWith("```")) {
      int firstLineEnd = trimmed.indexOf('\n');
      if (firstLineEnd >= 0) {
        trimmed = trimmed.substring(firstLineEnd + 1);
      } else {
        trimmed = trimmed.substring(3);
      }
      int fenceIndex = trimmed.lastIndexOf("```");
      if (fenceIndex >= 0) {
        trimmed = trimmed.substring(0, fenceIndex);
      }
      return trimmed.trim();
    }
    int firstBrace = trimmed.indexOf('{');
    int lastBrace = trimmed.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      return trimmed.substring(firstBrace, lastBrace + 1).trim();
    }
    return trimmed;
  }

  private Optional<JsonNode> attemptAutoCloseJson(String content) {
    if (content == null || content.isBlank()) {
      return Optional.empty();
    }
    String trimmed = content.trim();
    int start = trimmed.indexOf('{');
    if (start < 0) {
      return Optional.empty();
    }
    String candidate = trimmed.substring(start);
    String repaired = autoCloseJson(candidate);
    if (repaired == null || repaired.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readTree(repaired));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private String autoCloseJson(String input) {
    StringBuilder builder = new StringBuilder();
    List<Character> stack = new ArrayList<>();
    boolean inString = false;
    boolean escape = false;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      builder.append(c);
      if (escape) {
        escape = false;
        continue;
      }
      if (c == '\\') {
        escape = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (inString) {
        continue;
      }
      if (c == '{') {
        stack.add('}');
      } else if (c == '[') {
        stack.add(']');
      } else if (c == '}' || c == ']') {
        if (!stack.isEmpty() && stack.get(stack.size() - 1) == c) {
          stack.remove(stack.size() - 1);
        }
      }
    }
    if (inString) {
      builder.append('"');
    }
    String cleaned = removeTrailingCommas(builder.toString());
    StringBuilder closed = new StringBuilder(cleaned);
    for (int i = stack.size() - 1; i >= 0; i--) {
      closed.append(stack.get(i));
    }
    return removeTrailingCommas(closed.toString());
  }

  private String removeTrailingCommas(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String cleaned = value;
    String previous;
    do {
      previous = cleaned;
      cleaned = cleaned.replaceAll(",\\s*([}\\]])", "$1");
      cleaned = cleaned.replaceAll(",\\s*$", "");
    } while (!cleaned.equals(previous));
    return cleaned;
  }

  private record PageSignals(String text, String links) {
  }

  private record AuthorPageDiscovery(String url, String summary) {
  }

  private record ParseAttempt(Optional<JsonNode> node, String errorMessage, String sanitized) {
    private static ParseAttempt success(JsonNode node) {
      return new ParseAttempt(Optional.ofNullable(node), null, null);
    }

    private static ParseAttempt failure(String errorMessage, String sanitized) {
      return new ParseAttempt(Optional.empty(), errorMessage, sanitized);
    }
  }
}
