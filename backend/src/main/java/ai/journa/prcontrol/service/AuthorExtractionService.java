package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorExtractionService {
  private final HtmlFetchService htmlFetchService;
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ObjectMapper objectMapper;

  public AuthorExtractionService(HtmlFetchService htmlFetchService,
                                 ArticleAuthorExtractionRepository extractionRepository,
                                 ObjectMapper objectMapper) {
    this.htmlFetchService = htmlFetchService;
    this.extractionRepository = extractionRepository;
    this.objectMapper = objectMapper;
  }

  public ExtractionResult extractForArticle(Article article) {
    if (article == null || article.getUrl() == null || article.getUrl().isBlank()) {
      return new ExtractionResult(FetchStatus.SKIPPED, null, List.of(), "NO_URL", "Missing article URL");
    }
    Optional<String> html = htmlFetchService.fetchHtml(article.getUrl());
    if (html.isEmpty()) {
      return new ExtractionResult(FetchStatus.FAILED, null, List.of(), "FETCH_FAILED", "Unable to fetch article HTML");
    }
    Document document = Jsoup.parse(html.get());
    List<Candidate> candidates = new ArrayList<>();
    String authorRaw = null;

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
    Elements meta = document.select("meta[name=author], meta[property=article:author], meta[name=twitter:creator]");
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
    Elements elements = document.select("[class*=byline], [class*=author], [itemprop=author]");
    for (Element element : elements) {
      String text = element.text();
      if (text != null && !text.isBlank()) {
        candidates.add(new Candidate(text.trim(), 50, "HTML"));
        break;
      }
    }
    return candidates;
  }

  public record Candidate(String name, int confidence, String method) {
  }

  public record ExtractionResult(FetchStatus fetchStatus, String authorRaw, List<Candidate> candidates, String errorCode, String errorMessage) {
  }
}
