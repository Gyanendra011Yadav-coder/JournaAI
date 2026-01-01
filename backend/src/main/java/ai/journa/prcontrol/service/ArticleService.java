package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.ArticleStatus;
import ai.journa.prcontrol.domain.AuthorExtractionMethod;
import ai.journa.prcontrol.domain.AuthorExtractionStatus;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.LensSource;
import ai.journa.prcontrol.domain.MatchMethod;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.AdminArticleUpdateRequest;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import ai.journa.prcontrol.repository.NewsCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final BeatRepository beatRepository;
  private final NewsCacheRepository newsCacheRepository;
  private final AuditService auditService;
  private final SearchRateLimiter searchRateLimiter;
  private final ObjectMapper objectMapper;
  private final ArticleJournalistRepository articleJournalistRepository;
  private final ArticleAuthorExtractionRepository articleAuthorExtractionRepository;
  private final JournalistRepository journalistRepository;

  public ArticleService(ArticleRepository articleRepository,
                        BeatRepository beatRepository,
                        NewsCacheRepository newsCacheRepository,
                        AuditService auditService,
                        SearchRateLimiter searchRateLimiter,
                        ObjectMapper objectMapper,
                        ArticleJournalistRepository articleJournalistRepository,
                        ArticleAuthorExtractionRepository articleAuthorExtractionRepository,
                        JournalistRepository journalistRepository) {
    this.articleRepository = articleRepository;
    this.beatRepository = beatRepository;
    this.newsCacheRepository = newsCacheRepository;
    this.auditService = auditService;
    this.searchRateLimiter = searchRateLimiter;
    this.objectMapper = objectMapper;
    this.articleJournalistRepository = articleJournalistRepository;
    this.articleAuthorExtractionRepository = articleAuthorExtractionRepository;
    this.journalistRepository = journalistRepository;
  }

  public Page<Article> searchArticles(User actor,
                                      Long beatId,
                                      String category,
                                      LensSource lensSource,
                                      ArticleStatus status,
                                      String country,
                                      Instant from,
                                      Instant to,
                                      int page,
                                      int size,
                                      int maxPerMinute) {
    searchRateLimiter.enforceSearchLimit(actor != null ? actor.getEmail() : null, maxPerMinute);
    auditService.record(actor, "SEARCH", "articles", null, beatId != null ? beatId.toString() : null);
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "providerPublishedAtUtc"));
    Specification<Article> spec = Specification.where(null);
    if (beatId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("beat").get("id"), beatId));
    }
    if (category != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
    }
    if (lensSource != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("lensSource"), lensSource));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (country != null && !country.isBlank()) {
      String normalized = country.trim().toLowerCase();
      spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("sourceCountry")), normalized));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("providerPublishedAtUtc"), from));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("providerPublishedAtUtc"), to));
    }
    return articleRepository.findAll(spec, pageable);
  }

  public Page<Article> searchArticlesByJournalist(User actor,
                                                  Long journalistId,
                                                  ArticleStatus status,
                                                  Instant from,
                                                  Instant to,
                                                  int page,
                                                  int size,
                                                  int maxPerMinute) {
    searchRateLimiter.enforceSearchLimit(actor != null ? actor.getEmail() : null, maxPerMinute);
    auditService.record(actor, "SEARCH", "journalist-articles", null, journalistId != null ? journalistId.toString() : null);
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "providerPublishedAtUtc"));
    if (journalistId == null) {
      return Page.empty(pageable);
    }
    var articleIds = articleJournalistRepository.findByJournalistId(journalistId).stream()
        .map(link -> link.getArticle() != null ? link.getArticle().getId() : null)
        .filter(id -> id != null)
        .distinct()
        .toList();
    if (articleIds.isEmpty()) {
      return Page.empty(pageable);
    }
    Specification<Article> spec = (root, query, cb) -> root.get("id").in(articleIds);
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("providerPublishedAtUtc"), from));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("providerPublishedAtUtc"), to));
    }
    return articleRepository.findAll(spec, pageable);
  }

  public Article getArticle(Long id, User actor) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    auditService.record(actor, "VIEW", "article", null, id.toString());
    return article;
  }

  public Article publishArticle(Long id, User actor) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    article.setStatus(ArticleStatus.PUBLISHED);
    article.setPublishedBy(actor);
    article.setInternalPublishedAtUtc(Instant.now());
    articleRepository.save(article);
    auditService.record(actor, "PUBLISH", "article", null, id.toString());
    return article;
  }

  public Article unpublishArticle(Long id, User actor) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    article.setStatus(ArticleStatus.INGESTED);
    article.setPublishedBy(null);
    article.setInternalPublishedAtUtc(null);
    articleRepository.save(article);
    auditService.record(actor, "UNPUBLISH", "article", null, id.toString());
    return article;
  }

  public Article createManualArticle(ManualArticleRequest request, User actor) {
    Beat beat = beatRepository.findById(request.getBeatId())
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    String url = request.getUrl().trim();
    if (articleRepository.findByUrl(url).isPresent()) {
      throw new IllegalStateException("Article already exists");
    }
    Article article = new Article();
    article.setProviderType(ProviderType.MANUAL);
    article.setProviderArticleId("manual-" + UUID.randomUUID());
    article.setBeat(beat);
    article.setTitle(request.getTitle().trim());
    article.setDescription(request.getSummary());
    article.setContent(request.getSummary());
    article.setUrl(url);
    article.setSourceName(request.getSourceName());
    article.setProviderPublishedAtUtc(request.getPublishedAtUtc() != null ? request.getPublishedAtUtc() : Instant.now());
    article.setRawPayloadJsonb(buildManualPayload(request));
    Article saved = articleRepository.save(article);
    auditService.record(actor, "MANUAL_INGEST", "article", Map.of("title", saved.getTitle()), saved.getId().toString());
    return saved;
  }

  public Article updateArticle(Long id, AdminArticleUpdateRequest request, User actor) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    Map<String, Object> changes = new LinkedHashMap<>();
    if (request.getTitle() != null) {
      String trimmed = request.getTitle().trim();
      article.setTitle(trimmed);
      changes.put("title", trimmed);
    }
    if (request.getDescription() != null) {
      article.setDescription(request.getDescription().trim());
      changes.put("description", request.getDescription());
    }
    if (request.getContent() != null) {
      article.setContent(request.getContent().trim());
      changes.put("content", request.getContent());
    }
    if (request.getImageUrl() != null) {
      article.setImageUrl(request.getImageUrl().trim());
      changes.put("imageUrl", request.getImageUrl());
    }
    if (request.getSourceName() != null) {
      article.setSourceName(request.getSourceName().trim());
      changes.put("sourceName", request.getSourceName());
    }
    if (request.getSourceUrl() != null) {
      article.setSourceUrl(request.getSourceUrl().trim());
      changes.put("sourceUrl", request.getSourceUrl());
    }
    if (request.getSourceCountry() != null) {
      article.setSourceCountry(request.getSourceCountry().trim());
      changes.put("sourceCountry", request.getSourceCountry());
    }
    if (request.getCategory() != null) {
      article.setCategory(request.getCategory().trim());
      changes.put("category", request.getCategory());
    }
    if (request.getPublishedAtUtc() != null) {
      Instant publishedAt = parseInstant(request.getPublishedAtUtc());
      article.setProviderPublishedAtUtc(publishedAt);
      changes.put("publishedAtUtc", publishedAt != null ? publishedAt.toString() : null);
    }
    articleRepository.save(article);
    if (hasText(request.getAuthorRaw())) {
      saveManualAuthorExtraction(article, request.getAuthorRaw().trim());
      changes.put("authorRaw", request.getAuthorRaw().trim());
    }
    if (request.getJournalistId() != null) {
      applyManualJournalist(article, request.getJournalistId(), request.getJournalistMatchConfidence());
      changes.put("journalistId", request.getJournalistId());
    }
    auditService.record(actor, "UPDATE", "article", changes.isEmpty() ? null : changes, article.getId().toString());
    return article;
  }

  public Optional<Instant> getLastRefreshedAt(String cacheKey) {
    return newsCacheRepository.findByCacheKey(cacheKey)
        .map(cache -> cache.getLastSuccessAtUtc());
  }

  public Optional<Boolean> isStaleCache(String cacheKey) {
    return newsCacheRepository.findByCacheKey(cacheKey)
        .map(cache -> cache.getLastErrorCode() != null && !cache.getLastErrorCode().isBlank());
  }

  private String buildManualPayload(ManualArticleRequest request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("manual", true);
    if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
      payload.put("author", request.getAuthor());
    }
    if (request.getSourceName() != null && !request.getSourceName().isBlank()) {
      payload.put("source", request.getSourceName());
    }
    if (request.getSummary() != null && !request.getSummary().isBlank()) {
      payload.put("summary", request.getSummary());
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      return "{\"manual\":true}";
    }
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid timestamp format");
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private void saveManualAuthorExtraction(Article article, String authorRaw) {
    ArticleAuthorExtraction extraction = new ArticleAuthorExtraction();
    extraction.setArticle(article);
    extraction.setAuthorRaw(authorRaw);
    extraction.setMethod(AuthorExtractionMethod.MANUAL);
    extraction.setStatus(AuthorExtractionStatus.SUCCESS);
    extraction.setFetchStatus(FetchStatus.SUCCESS);
    extraction.setConfidence(100);
    extraction.setNonPersonAuthor(false);
    extraction.setExtractedAt(Instant.now());
    articleAuthorExtractionRepository.save(extraction);
  }

  private void applyManualJournalist(Article article, Long journalistId, Integer confidence) {
    if (journalistId == null) {
      return;
    }
    Journalist journalist = journalistRepository.findById(journalistId)
        .orElseThrow(() -> new IllegalStateException("Journalist not found"));
    ArticleJournalist link = articleJournalistRepository.findByArticleIdAndJournalistId(article.getId(), journalistId)
        .orElseGet(() -> {
          ArticleJournalist created = new ArticleJournalist();
          created.setArticle(article);
          created.setJournalist(journalist);
          return created;
        });
    link.setMatchConfidence(confidence != null ? confidence : 100);
    link.setMatchMethod(MatchMethod.ADMIN_APPROVED);
    articleJournalistRepository.save(link);
  }
}
