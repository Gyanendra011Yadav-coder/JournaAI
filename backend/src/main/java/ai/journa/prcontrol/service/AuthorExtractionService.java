package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.config.EnrichmentProperties;
import ai.journa.prcontrol.config.LlmProperties;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.AuthorClassification;
import ai.journa.prcontrol.domain.AuthorExtractionMethod;
import ai.journa.prcontrol.domain.AuthorExtractionStatus;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.service.llm.LlmClientService;
import ai.journa.prcontrol.service.llm.LlmRequest;
import ai.journa.prcontrol.service.llm.LlmResponse;
import ai.journa.prcontrol.service.llm.PromptFileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthorExtractionService {
  private static final Logger logger = LoggerFactory.getLogger(AuthorExtractionService.class);
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final Pattern ATTRIBUTION_PREFIX = Pattern.compile("(?i)^(by|reviewed by|edited by|written by|reported by)\\s*[:\\-]?\\s*(.+)$");
  private static final Pattern ATTRIBUTION_ONLY_PREFIX = Pattern.compile("(?i)^(by|reviewed by|edited by|written by|reported by)\\s*[:\\-]?\\s*$");
  private static final Pattern BYLINE_SPLIT = Pattern.compile("\\s+(\\||-|—|–)\\s+");
  private final HtmlFetchService htmlFetchService;
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ObjectMapper objectMapper;
  private final EnrichmentProperties enrichmentProperties;
  private final LlmClientService llmClientService;
  private final LlmProperties llmProperties;
  private final AuthorNormalizationService normalizationService;
  private final PromptFileService promptFileService;

  public AuthorExtractionService(HtmlFetchService htmlFetchService,
                                 ArticleAuthorExtractionRepository extractionRepository,
                                 ObjectMapper objectMapper,
                                 EnrichmentProperties enrichmentProperties,
                                 LlmClientService llmClientService,
                                 LlmProperties llmProperties,
                                 AuthorNormalizationService normalizationService,
                                 PromptFileService promptFileService) {
    this.htmlFetchService = htmlFetchService;
    this.extractionRepository = extractionRepository;
    this.objectMapper = objectMapper;
    this.enrichmentProperties = enrichmentProperties;
    this.llmClientService = llmClientService;
    this.llmProperties = llmProperties;
    this.normalizationService = normalizationService;
    this.promptFileService = promptFileService;
  }

  public ExtractionResult extractForArticle(Article article) {
    if (article == null || article.getUrl() == null || article.getUrl().isBlank()) {
      return ExtractionResult.skipped("NO_URL", "Missing article URL", new ExtractionSignals(null, null, null, null));
    }
    SignalCollector signals = new SignalCollector();
    List<Candidate> candidates = new ArrayList<>();
    String authorRaw = null;

    signals.pageTextSnippet = pickSnippet(article.getContent(), 600);
    if (signals.pageTextSnippet == null) {
      signals.pageTextSnippet = pickSnippet(article.getDescription(), 400);
    }

    extractFromText(article.getContent(), "CONTENT", 60, candidates, signals);
    extractFromText(article.getDescription(), "DESCRIPTION", 55, candidates, signals);
    if (!candidates.isEmpty()) {
      authorRaw = candidates.get(0).name();
      ExtractionResult deterministic = deterministicResult(article, authorRaw, candidates, signals);
      if (shouldUseLlmForBureau(deterministic)) {
        ExtractionResult llmResult = llmFallback(article, signals.toSignals());
        if (llmResult.fetchStatus() == FetchStatus.SUCCESS) {
          return llmResult;
        }
      }
      return deterministic;
    }

    Optional<String> html = htmlFetchService.fetchHtml(article.getUrl());
    if (html.isEmpty()) {
      logger.warn("Author extraction failed articleId={} reason=HTML_FETCH_FAILED url={}",
          article.getId(), article.getUrl());
    } else {
      Document document = Jsoup.parse(html.get());
      extractFromDocument(document, signals, candidates);
      if (!candidates.isEmpty()) {
        authorRaw = candidates.get(0).name();
      }
      if (candidates.isEmpty() && !htmlFetchService.wasRendered(article.getUrl())) {
        logger.info("Author extraction retry with Playwright articleId={} reason=NO_BYLINE", article.getId());
        Optional<String> rendered = htmlFetchService.fetchHtmlWithPlaywright(article.getUrl());
        if (rendered.isPresent()) {
          Document renderedDoc = Jsoup.parse(rendered.get());
          SignalCollector renderedSignals = new SignalCollector();
          List<Candidate> renderedCandidates = new ArrayList<>();
          extractFromDocument(renderedDoc, renderedSignals, renderedCandidates);
          if (!renderedCandidates.isEmpty()) {
            candidates.clear();
            candidates.addAll(renderedCandidates);
            authorRaw = candidates.get(0).name();
          }
          mergeSignals(signals, renderedSignals);
        }
      }
    }

    if (candidates.isEmpty()) {
      return llmFallback(article, signals.toSignals());
    }
    return deterministicResult(article, authorRaw, candidates, signals);
  }

  public ArticleAuthorExtraction persistExtraction(Article article, ExtractionResult result, String candidatesJsonb) {
    ArticleAuthorExtraction extraction = new ArticleAuthorExtraction();
    extraction.setArticle(article);
    extraction.setAuthorRaw(result.authorRaw());
    extraction.setCandidatesJsonb(candidatesJsonb);
    extraction.setAuthorsJsonb(result.authorsJsonb());
    extraction.setClassification(result.classification());
    extraction.setMethod(result.method());
    extraction.setConfidence(result.confidence());
    extraction.setEvidenceJsonb(result.evidenceJsonb());
    extraction.setStatus(result.status());
    extraction.setAttempts(result.attempts());
    extraction.setContentHash(result.contentHash());
    extraction.setNonPersonAuthor(result.classification() == AuthorClassification.BUREAU);
    extraction.setFetchStatus(result.fetchStatus());
    extraction.setExtractedAt(Instant.now());
    extraction.setErrorCode(result.errorCode());
    extraction.setErrorMessage(result.errorMessage());
    return extractionRepository.save(extraction);
  }

  private ExtractionResult deterministicResult(Article article, String authorRaw, List<Candidate> candidates, SignalCollector signals) {
    List<String> names = candidates.stream().map(Candidate::name).toList();
    List<AuthorNormalizationService.Candidate> normalized = normalizationService.normalizeCandidates(names);
    AuthorClassification classification = normalized.isEmpty()
        ? AuthorClassification.UNKNOWN
        : normalized.stream().allMatch(AuthorNormalizationService.Candidate::nonPerson)
            ? AuthorClassification.BUREAU
            : AuthorClassification.PERSON;
    boolean sourceMatch = isSourceMatch(article, authorRaw);
    if (sourceMatch) {
      classification = AuthorClassification.BUREAU;
    }
    List<Attribution> authors = normalized.stream()
        .filter(candidate -> !candidate.nonPerson())
        .map(candidate -> new Attribution(candidate.name(), 70, "deterministic"))
        .toList();
    if (classification == AuthorClassification.BUREAU) {
      authors = List.of();
    }
    Attribution bureau = classification == AuthorClassification.BUREAU
        ? new Attribution(authorRaw != null ? authorRaw : "Bureau", 70, "deterministic")
        : null;
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("signals", signals.toSignals());
    return new ExtractionResult(
        FetchStatus.SUCCESS,
        AuthorExtractionMethod.DETERMINISTIC,
        classification,
        classification == AuthorClassification.UNKNOWN ? AuthorExtractionStatus.NEEDS_REVIEW : AuthorExtractionStatus.SUCCESS,
        authorRaw,
        candidates,
        authors,
        bureau,
        0,
        buildContentHash(article, signals.toSignals()),
        null,
        null,
        evidence,
        signals.toSignals()
    );
  }

  private ExtractionResult llmFallback(Article article, ExtractionSignals signals) {
    if (!llmProperties.isEnabled() || !llmProperties.getAuthorExtraction().isEnabled()) {
      return ExtractionResult.skipped("LLM_DISABLED", "LLM fallback disabled", signals);
    }
    String contentHash = buildContentHash(article, signals);
    Instant now = Instant.now();
    LlmProperties.AuthorExtraction config = llmProperties.getAuthorExtraction();

    Optional<ArticleAuthorExtraction> cached = extractionRepository
        .findFirstByArticleIdAndContentHashAndMethodOrderByExtractedAtDesc(
            article.getId(),
            contentHash,
            AuthorExtractionMethod.LLM
        );
    if (cached.isPresent()) {
      ArticleAuthorExtraction previous = cached.get();
      if (previous.getExtractedAt() != null
          && previous.getExtractedAt().isAfter(now.minus(Duration.ofDays(config.getCacheDays())))) {
        return ExtractionResult.fromExisting(previous, signals);
      }
      if (previous.getAttempts() >= config.getMaxAttempts()) {
        return ExtractionResult.needsReview("LLM_MAX_ATTEMPTS", "LLM attempts exceeded", contentHash, signals, previous.getAttempts());
      }
      if (previous.getExtractedAt() != null
          && previous.getExtractedAt().isAfter(now.minus(Duration.ofMinutes(config.getCooldownMinutes())))) {
        return ExtractionResult.skipped("LLM_COOLDOWN", "LLM cooldown active", signals);
      }
    }

    String userPrompt = buildUserPrompt(article, signals);
    LlmRequest request = new LlmRequest(
        promptFileService.getAuthorSystemPrompt(),
        userPrompt,
        config.getTemperature(),
        config.getMaxTokens()
    );
    Optional<LlmResponse> response = llmClientService.generate(request, null);
    int attempts = cached.map(ArticleAuthorExtraction::getAttempts).orElse(0) + 1;
    if (response.isEmpty()) {
      return ExtractionResult.failed("LLM_UNAVAILABLE", "LLM provider unavailable", contentHash, signals, attempts);
    }
    return parseLlmResponse(article, response.get(), contentHash, attempts, signals);
  }

  private boolean shouldUseLlmForBureau(ExtractionResult result) {
    if (result == null) {
      return false;
    }
    if (!llmProperties.isEnabled() || !llmProperties.getAuthorExtraction().isEnabled()) {
      return false;
    }
    return result.classification() == AuthorClassification.BUREAU;
  }

  private ExtractionResult parseLlmResponse(Article article,
                                            LlmResponse response,
                                            String contentHash,
                                            int attempts,
                                            ExtractionSignals signals) {
    String content = response.content();
    try {
      JsonNode node = objectMapper.readTree(content);
      String classificationRaw = node.path("classification").asText("");
      AuthorClassification classification = parseClassification(classificationRaw);
      List<Attribution> authors = new ArrayList<>();
      for (JsonNode authorNode : node.path("authors")) {
        String name = authorNode.path("name").asText(null);
        if (name == null || name.isBlank()) {
          continue;
        }
        int confidence = authorNode.path("confidence").asInt(50);
        String evidence = authorNode.path("evidence").asText(null);
        authors.add(new Attribution(name.trim(), confidence, evidence));
      }
      Attribution bureau = null;
      JsonNode bureauNode = node.path("bureau");
      if (!bureauNode.isMissingNode() && !bureauNode.isNull()) {
        String name = bureauNode.path("name").asText(null);
        if (name != null && !name.isBlank()) {
          bureau = new Attribution(name.trim(), bureauNode.path("confidence").asInt(50), bureauNode.path("evidence").asText(null));
        }
      }
      boolean needsReview = node.path("needs_human_review").asBoolean(false);
      ExtractionResult normalized = normalizeLlmOutput(article, contentHash, attempts, signals, classification, authors, bureau, needsReview);
      Map<String, Object> evidence = normalized.evidence();
      evidence.put("llm_raw", content);
      return normalized.withEvidence(evidence);
    } catch (Exception ex) {
      return ExtractionResult.failed("LLM_PARSE_ERROR", "Unable to parse LLM response", contentHash, signals, attempts);
    }
  }

  private ExtractionResult normalizeLlmOutput(Article article,
                                              String contentHash,
                                              int attempts,
                                              ExtractionSignals signals,
                                              AuthorClassification classification,
                                              List<Attribution> authors,
                                              Attribution bureau,
                                              boolean needsReview) {
    AuthorClassification normalizedClassification = classification;
    if (normalizedClassification == AuthorClassification.PERSON && authors.isEmpty()) {
      normalizedClassification = AuthorClassification.UNKNOWN;
    }
    if (normalizedClassification == AuthorClassification.BUREAU && bureau == null) {
      normalizedClassification = AuthorClassification.UNKNOWN;
    }
    if (normalizedClassification == AuthorClassification.PERSON) {
      boolean allNonPerson = authors.stream().allMatch(author -> normalizationService.isNonPerson(author.name()));
      if (allNonPerson) {
        normalizedClassification = AuthorClassification.BUREAU;
        bureau = authors.isEmpty() ? null : new Attribution(authors.get(0).name(), authors.get(0).confidence(), "llm");
        authors = List.of();
      }
    }
    String primaryName = null;
    if (normalizedClassification == AuthorClassification.PERSON && !authors.isEmpty()) {
      primaryName = authors.get(0).name();
    } else if (bureau != null) {
      primaryName = bureau.name();
    }
    if (isSourceMatch(article, primaryName)) {
      int confidence = authors.isEmpty() ? 60 : authors.get(0).confidence();
      normalizedClassification = AuthorClassification.BUREAU;
      bureau = new Attribution(primaryName != null ? primaryName : "Bureau", confidence, "source-match");
      authors = List.of();
    }
    AuthorExtractionStatus status = normalizedClassification == AuthorClassification.UNKNOWN || needsReview
        ? AuthorExtractionStatus.NEEDS_REVIEW
        : AuthorExtractionStatus.SUCCESS;
    String authorRaw = normalizedClassification == AuthorClassification.PERSON
        ? authors.stream().map(Attribution::name).findFirst().orElse(null)
        : bureau != null ? bureau.name() : null;
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("signals", signals);
    return new ExtractionResult(
        FetchStatus.SUCCESS,
        AuthorExtractionMethod.LLM,
        normalizedClassification,
        status,
        authorRaw,
        List.of(),
        authors,
        bureau,
        attempts,
        contentHash,
        null,
        null,
        evidence,
        signals
    );
  }

  private AuthorClassification parseClassification(String raw) {
    if (raw == null) {
      return AuthorClassification.UNKNOWN;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    try {
      return AuthorClassification.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return AuthorClassification.UNKNOWN;
    }
  }

  private String buildUserPrompt(Article article, ExtractionSignals signals) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("ARTICLE_URL", safe(article.getUrl()));
    values.put("SOURCE_NAME", safe(article.getSourceName()));
    values.put("JSONLD_SNIPPETS", safe(signals.jsonLdSnippets()));
    values.put("META_TAGS", safe(signals.metaTags()));
    values.put("BYLINE_SNIPPET", safe(signals.bylineSnippet()));
    values.put("PAGE_TEXT_SNIPPET", safe(signals.pageTextSnippet()));
    return promptFileService.renderAuthorUserPrompt(values);
  }

  private String buildContentHash(Article article, ExtractionSignals signals) {
    String payload = String.join("|",
        safe(article.getUrl()),
        safe(article.getTitle()),
        safe(article.getDescription()),
        safe(signals.jsonLdSnippets()),
        safe(signals.metaTags()),
        safe(signals.bylineSnippet()),
        safe(signals.pageTextSnippet())
    );
    return sha256(payload);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception ex) {
      return Integer.toHexString(value.hashCode());
    }
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private void extractFromText(String text, String method, int confidence, List<Candidate> candidates, SignalCollector signals) {
    if (text == null || text.isBlank()) {
      return;
    }
    Matcher matcher = ATTRIBUTION_PREFIX.matcher(text.stripLeading());
    if (!matcher.find()) {
      return;
    }
    String name = matcher.group(2);
    if (name == null) {
      return;
    }
    String trimmed = extractLeadingName(name);
    if (trimmed.isBlank()) {
      return;
    }
    if (signals != null && signals.bylineSnippet == null) {
      signals.bylineSnippet = pickSnippet(matcher.group(0), 200);
    }
    String[] parts = BYLINE_SPLIT.split(trimmed, 2);
    String candidate = parts.length > 0 ? parts[0].trim() : trimmed;
    if (!candidate.isBlank()) {
      candidates.add(new Candidate(candidate, confidence, method));
    }
  }

  private void extractFromDocument(Document document, SignalCollector signals, List<Candidate> candidates) {
    if (document == null) {
      return;
    }
    candidates.addAll(extractFromJsonLd(document, signals));
    candidates.addAll(extractFromMeta(document, signals));
    if (candidates.isEmpty()) {
      candidates.addAll(extractFromByline(document, signals));
    }
    if (signals.pageTextSnippet == null) {
      signals.pageTextSnippet = pickSnippet(document.text(), 800);
    }
  }

  private void mergeSignals(SignalCollector base, SignalCollector incoming) {
    if (incoming == null || base == null) {
      return;
    }
    if (incoming.jsonLdSnippet != null && !incoming.jsonLdSnippet.isBlank()) {
      base.jsonLdSnippet = incoming.jsonLdSnippet;
    }
    if (incoming.metaTags != null && !incoming.metaTags.isBlank()) {
      base.metaTags = incoming.metaTags;
    }
    if (incoming.bylineSnippet != null && !incoming.bylineSnippet.isBlank()) {
      base.bylineSnippet = incoming.bylineSnippet;
    }
    if (incoming.pageTextSnippet != null && !incoming.pageTextSnippet.isBlank()) {
      base.pageTextSnippet = incoming.pageTextSnippet;
    }
  }

  private List<Candidate> extractFromJsonLd(Document document, SignalCollector signals) {
    List<Candidate> candidates = new ArrayList<>();
    Elements scripts = document.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      try {
        JsonNode node = objectMapper.readTree(script.html());
        if (signals != null && signals.jsonLdSnippet == null) {
          signals.jsonLdSnippet = pickSnippet(script.html(), 1200);
        }
        extractAuthorNodes(node, "JSON_LD", 90, candidates);
      } catch (Exception ignored) {
      }
    }
    return candidates;
  }

  private void extractAuthorNodes(JsonNode node, String method, int confidence, List<Candidate> candidates) {
    if (node == null) {
      return;
    }
    if (node.isArray()) {
      node.forEach(child -> extractAuthorNodes(child, method, confidence, candidates));
      return;
    }
    JsonNode authorNode = node.get("author");
    if (authorNode != null) {
      if (authorNode.isArray()) {
        authorNode.forEach(author -> addAuthorCandidate(author, method, confidence, candidates));
      } else {
        addAuthorCandidate(authorNode, method, confidence, candidates);
      }
    }
  }

  private void addAuthorCandidate(JsonNode authorNode, String method, int confidence, List<Candidate> candidates) {
    if (authorNode == null) {
      return;
    }
    if (authorNode.isTextual()) {
      candidates.add(new Candidate(authorNode.asText(), confidence, method));
      return;
    }
    JsonNode nameNode = authorNode.get("name");
    if (nameNode != null && nameNode.isTextual()) {
      candidates.add(new Candidate(nameNode.asText(), confidence, method));
    }
  }

  private List<Candidate> extractFromMeta(Document document, SignalCollector signals) {
    List<Candidate> candidates = new ArrayList<>();
    Elements meta = document.select("meta[name=author], meta[property=article:author], meta[name=twitter:creator], meta[name=reviewer]");
    for (Element element : meta) {
      String content = element.attr("content");
      if (content != null && !content.isBlank()) {
        if (signals != null && signals.metaTags == null) {
          signals.metaTags = "author=" + content.trim();
        }
        candidates.add(new Candidate(content.trim(), 70, "META"));
      }
    }
    return candidates;
  }

  private List<Candidate> extractFromByline(Document document, SignalCollector signals) {
    List<Candidate> candidates = new ArrayList<>();
    Elements elements = document.select("[class*=byline], [class*=author], [class*=review], [class*=reviewed], [itemprop=author], [itemprop=reviewedBy]");
    for (Element element : elements) {
      String text = element.text();
      if (text != null && !text.isBlank()) {
        if (signals != null && signals.bylineSnippet == null) {
          signals.bylineSnippet = pickSnippet(text, 200);
        }
        if (extractAttributedName(text, "HTML", 55, candidates)) {
          break;
        }
        candidates.add(new Candidate(text.trim(), 45, "HTML"));
        break;
      }
    }
    return candidates;
  }

  private boolean extractAttributedName(String text, String method, int confidence, List<Candidate> candidates) {
    String[] lines = text.split("\\R+");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isBlank()) {
        continue;
      }
      Matcher matcher = ATTRIBUTION_PREFIX.matcher(line);
      if (matcher.find()) {
        String candidate = matcher.group(2).trim();
        String extracted = extractLeadingName(candidate);
        String[] parts = BYLINE_SPLIT.split(extracted, 2);
        String name = parts.length > 0 ? parts[0].trim() : extracted;
        if (name.isBlank() && i + 1 < lines.length) {
          name = extractLeadingName(lines[i + 1].trim());
        }
        if (!name.isBlank()) {
          candidates.add(new Candidate(name, confidence, method));
          return true;
        }
      }
      Matcher prefixOnly = ATTRIBUTION_ONLY_PREFIX.matcher(line);
      if (prefixOnly.find() && i + 1 < lines.length) {
        String next = lines[i + 1].trim();
        if (!next.isBlank()) {
          String extracted = extractLeadingName(next);
          String[] parts = BYLINE_SPLIT.split(extracted, 2);
          String name = parts.length > 0 ? parts[0].trim() : extracted;
          if (!name.isBlank()) {
            candidates.add(new Candidate(name, confidence, method));
            return true;
          }
        }
      }
    }
    return false;
  }

  private String extractLeadingName(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    Matcher matcher = Pattern.compile("([A-Z][\\p{L}\\.'-]+(?:\\s+[A-Z][\\p{L}\\.'-]+){0,2})").matcher(trimmed);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return trimmed;
  }

  private String pickSnippet(String value, int max) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isBlank()) {
      return null;
    }
    if (trimmed.length() <= max) {
      return trimmed;
    }
    return trimmed.substring(0, max);
  }

  private boolean isSourceMatch(Article article, String authorRaw) {
    if (authorRaw == null || authorRaw.isBlank()) {
      return false;
    }
    String sourceName = article != null ? article.getSourceName() : null;
    if (sourceName != null && !sourceName.isBlank()) {
      String normalizedName = normalizeKey(authorRaw);
      String normalizedSource = normalizeKey(sourceName);
      if (!normalizedName.isBlank() && !normalizedSource.isBlank()) {
        if (normalizedName.equals(normalizedSource)
            || normalizedName.contains(normalizedSource)
            || normalizedSource.contains(normalizedName)) {
          return true;
        }
      }
    }
    String domainKey = extractDomainKey(article != null ? article.getSourceUrl() : null);
    if (domainKey != null && !domainKey.isBlank()) {
      String normalizedName = normalizeKey(authorRaw);
      if (!normalizedName.isBlank() && normalizedName.contains(domainKey)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeKey(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private String extractDomainKey(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      return null;
    }
    try {
      String host = URI.create(sourceUrl).getHost();
      if (host == null) {
        return null;
      }
      String normalized = host.toLowerCase(Locale.ROOT);
      if (normalized.startsWith("www.")) {
        normalized = normalized.substring(4);
      }
      int dotIndex = normalized.indexOf('.');
      String core = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
      return normalizeKey(core);
    } catch (Exception ex) {
      return null;
    }
  }

  private static class SignalCollector {
    private String jsonLdSnippet;
    private String metaTags;
    private String bylineSnippet;
    private String pageTextSnippet;

    private ExtractionSignals toSignals() {
      return new ExtractionSignals(jsonLdSnippet, metaTags, bylineSnippet, pageTextSnippet);
    }
  }

  public record Candidate(String name, int confidence, String method) {
  }

  public record Attribution(String name, int confidence, String evidence) {
  }

  public record ExtractionSignals(String jsonLdSnippets, String metaTags, String bylineSnippet, String pageTextSnippet) {
  }

  public record ExtractionResult(FetchStatus fetchStatus,
                                 AuthorExtractionMethod method,
                                 AuthorClassification classification,
                                 AuthorExtractionStatus status,
                                 String authorRaw,
                                 List<Candidate> candidates,
                                 List<Attribution> authors,
                                 Attribution bureau,
                                 int attempts,
                                 String contentHash,
                                 String errorCode,
                                 String errorMessage,
                                 Map<String, Object> evidence,
                                 ExtractionSignals signals) {

    public static ExtractionResult skipped(String errorCode, String errorMessage, ExtractionSignals signals) {
      return new ExtractionResult(FetchStatus.SKIPPED, AuthorExtractionMethod.DETERMINISTIC, AuthorClassification.UNKNOWN,
          AuthorExtractionStatus.SKIPPED, null, List.of(), List.of(), null, 0, null, errorCode, errorMessage,
          new LinkedHashMap<>(), signals);
    }

    public static ExtractionResult failed(String errorCode, String errorMessage, String contentHash,
                                          ExtractionSignals signals, int attempts) {
      return new ExtractionResult(FetchStatus.FAILED, AuthorExtractionMethod.LLM, AuthorClassification.UNKNOWN,
          AuthorExtractionStatus.FAILED, null, List.of(), List.of(), null, attempts, contentHash, errorCode, errorMessage,
          new LinkedHashMap<>(), signals);
    }

    public static ExtractionResult needsReview(String errorCode, String errorMessage, String contentHash,
                                               ExtractionSignals signals, int attempts) {
      return new ExtractionResult(FetchStatus.SUCCESS, AuthorExtractionMethod.LLM, AuthorClassification.UNKNOWN,
          AuthorExtractionStatus.NEEDS_REVIEW, null, List.of(), List.of(), null, attempts, contentHash,
          errorCode, errorMessage, new LinkedHashMap<>(), signals);
    }

    public static ExtractionResult fromExisting(ArticleAuthorExtraction extraction, ExtractionSignals signals) {
      AuthorClassification classification = extraction.getClassification() != null
          ? extraction.getClassification()
          : AuthorClassification.UNKNOWN;
      AuthorExtractionStatus status = extraction.getStatus() != null
          ? extraction.getStatus()
          : AuthorExtractionStatus.SUCCESS;
      List<Attribution> authors = parseAuthorsJsonb(extraction.getAuthorsJsonb());
      Attribution bureau = classification == AuthorClassification.BUREAU && extraction.getAuthorRaw() != null
          ? new Attribution(extraction.getAuthorRaw(), extraction.getConfidence() != null ? extraction.getConfidence() : 70, "cached")
          : null;
      Map<String, Object> evidence = parseEvidenceJsonb(extraction.getEvidenceJsonb());
      return new ExtractionResult(
          extraction.getFetchStatus() != null ? extraction.getFetchStatus() : FetchStatus.SUCCESS,
          extraction.getMethod() != null ? extraction.getMethod() : AuthorExtractionMethod.LLM,
          classification,
          status,
          extraction.getAuthorRaw(),
          List.of(),
          authors,
          bureau,
          extraction.getAttempts(),
          extraction.getContentHash(),
          extraction.getErrorCode(),
          extraction.getErrorMessage(),
          evidence != null ? evidence : new LinkedHashMap<>(),
          signals
      );
    }

    public String authorsJsonb() {
      if (authors == null || authors.isEmpty()) {
        return null;
      }
      try {
        return JSON_MAPPER.writeValueAsString(authors);
      } catch (Exception ex) {
        return "[]";
      }
    }

    public String evidenceJsonb() {
      if (evidence == null || evidence.isEmpty()) {
        return null;
      }
      try {
        return JSON_MAPPER.writeValueAsString(evidence);
      } catch (Exception ex) {
        return "{}";
      }
    }

    private static List<Attribution> parseAuthorsJsonb(String payload) {
      if (payload == null || payload.isBlank()) {
        return List.of();
      }
      try {
        return JSON_MAPPER.readValue(payload, new TypeReference<List<Attribution>>() {});
      } catch (Exception ex) {
        return List.of();
      }
    }

    private static Map<String, Object> parseEvidenceJsonb(String payload) {
      if (payload == null || payload.isBlank()) {
        return new LinkedHashMap<>();
      }
      try {
        return JSON_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {});
      } catch (Exception ex) {
        return new LinkedHashMap<>();
      }
    }

    public int confidence() {
      int max = 0;
      if (authors != null) {
        for (Attribution author : authors) {
          max = Math.max(max, author.confidence());
        }
      }
      if (bureau != null) {
        max = Math.max(max, bureau.confidence());
      }
      return max;
    }

    public ExtractionResult withEvidence(Map<String, Object> updatedEvidence) {
      return new ExtractionResult(fetchStatus, method, classification, status, authorRaw, candidates, authors, bureau,
          attempts, contentHash, errorCode, errorMessage, updatedEvidence, signals);
    }
  }
}
