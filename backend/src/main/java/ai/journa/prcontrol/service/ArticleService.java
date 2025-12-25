package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.LensSource;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatRepository;
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

  public ArticleService(ArticleRepository articleRepository,
                        BeatRepository beatRepository,
                        NewsCacheRepository newsCacheRepository,
                        AuditService auditService,
                        SearchRateLimiter searchRateLimiter,
                        ObjectMapper objectMapper) {
    this.articleRepository = articleRepository;
    this.beatRepository = beatRepository;
    this.newsCacheRepository = newsCacheRepository;
    this.auditService = auditService;
    this.searchRateLimiter = searchRateLimiter;
    this.objectMapper = objectMapper;
  }

  public Page<Article> searchArticles(User actor,
                                      Long beatId,
                                      String category,
                                      LensSource lensSource,
                                      ArticleStatus status,
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
}
