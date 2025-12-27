package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.config.EnrichmentProperties;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthorExtractionService {
  private static final Logger logger = LoggerFactory.getLogger(AuthorExtractionService.class);
  private static final Pattern ATTRIBUTION_PREFIX = Pattern.compile("(?i)^(by|reviewed by|edited by|written by|reported by)\\s*[:\\-]?\\s*(.+)$");
  private static final Pattern BYLINE_SPLIT = Pattern.compile("\\s+(\\||-|—|–)\\s+");
  private final HtmlFetchService htmlFetchService;
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ObjectMapper objectMapper;
  private final EnrichmentProperties enrichmentProperties;

  public AuthorExtractionService(HtmlFetchService htmlFetchService,
                                 ArticleAuthorExtractionRepository extractionRepository,
                                 ObjectMapper objectMapper,
                                 EnrichmentProperties enrichmentProperties) {
    this.htmlFetchService = htmlFetchService;
    this.extractionRepository = extractionRepository;
    this.objectMapper = objectMapper;
    this.enrichmentProperties = enrichmentProperties;
  }

  public ExtractionResult extractForArticle(Article article) {
    if (article == null || article.getUrl() == null || article.getUrl().isBlank()) {
      return new ExtractionResult(FetchStatus.SKIPPED, null, List.of(), "NO_URL", "Missing article URL");
    }
    List<Candidate> candidates = new ArrayList<>();
    String authorRaw = null;

    extractFromText(article.getContent(), "CONTENT", 60, candidates);
    extractFromText(article.getDescription(), "DESCRIPTION", 55, candidates);
    if (!candidates.isEmpty()) {
      authorRaw = candidates.get(0).name();
      return new ExtractionResult(FetchStatus.SUCCESS, authorRaw, candidates, null, null);
    }

    if (!enrichmentProperties.isHtmlFetchEnabled()) {
      logger.info("Author extraction skipped articleId={} reason=HTML_FETCH_DISABLED url={}",
          article.getId(), article.getUrl());
      logger.info("Author extraction skipped articleId={} reason=NO_BYLINE_IN_API_FIELDS",
          article.getId());
      return new ExtractionResult(FetchStatus.SKIPPED, null, List.of(), "HTML_DISABLED", "HTML fetch disabled");
    }

    Optional<String> html = htmlFetchService.fetchHtml(article.getUrl());
    if (html.isEmpty()) {
      logger.warn("Author extraction failed articleId={} reason=HTML_FETCH_FAILED url={}",
          article.getId(), article.getUrl());
      return new ExtractionResult(FetchStatus.FAILED, null, List.of(), "FETCH_FAILED", "Unable to fetch article HTML");
    }
    Document document = Jsoup.parse(html.get());
    candidates.addAll(extractFromJsonLd(document));
    candidates.addAll(extractFromMeta(document));
    if (candidates.isEmpty()) {
      candidates.addAll(extractFromByline(document));
    }
    if (!candidates.isEmpty()) {
      authorRaw = candidates.get(0).name();
    }
    return new ExtractionResult(FetchStatus.SUCCESS, authorRaw, candidates, null, null);
  }

  public ArticleAuthorExtraction persistExtraction(Article article, ExtractionResult result, String candidatesJsonb, boolean nonPerson) {
    ArticleAuthorExtraction extraction = new ArticleAuthorExtraction();
    extraction.setArticle(article);
    extraction.setAuthorRaw(result.authorRaw());
    extraction.setCandidatesJsonb(candidatesJsonb);
    extraction.setNonPersonAuthor(nonPerson);
    extraction.setFetchStatus(result.fetchStatus());
    extraction.setExtractedAt(Instant.now());
    extraction.setErrorCode(result.errorCode());
    extraction.setErrorMessage(result.errorMessage());
    return extractionRepository.save(extraction);
  }

  private void extractFromText(String text, String method, int confidence, List<Candidate> candidates) {
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
    String[] parts = BYLINE_SPLIT.split(trimmed, 2);
    String candidate = parts.length > 0 ? parts[0].trim() : trimmed;
    if (!candidate.isBlank()) {
      candidates.add(new Candidate(candidate, confidence, method));
    }
  }

  private List<Candidate> extractFromJsonLd(Document document) {
    List<Candidate> candidates = new ArrayList<>();
    Elements scripts = document.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      try {
        JsonNode node = objectMapper.readTree(script.html());
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

  private List<Candidate> extractFromMeta(Document document) {
    List<Candidate> candidates = new ArrayList<>();
    Elements meta = document.select("meta[name=author], meta[property=article:author], meta[name=twitter:creator], meta[name=reviewer]");
    for (Element element : meta) {
      String content = element.attr("content");
      if (content != null && !content.isBlank()) {
        candidates.add(new Candidate(content.trim(), 70, "META"));
      }
    }
    return candidates;
  }

  private List<Candidate> extractFromByline(Document document) {
    List<Candidate> candidates = new ArrayList<>();
    Elements elements = document.select("[class*=byline], [class*=author], [class*=review], [class*=reviewed], [itemprop=author], [itemprop=reviewedBy]");
    for (Element element : elements) {
      String text = element.text();
      if (text != null && !text.isBlank()) {
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

  public record Candidate(String name, int confidence, String method) {
  }

  public record ExtractionResult(FetchStatus fetchStatus, String authorRaw, List<Candidate> candidates, String errorCode, String errorMessage) {
  }
}
