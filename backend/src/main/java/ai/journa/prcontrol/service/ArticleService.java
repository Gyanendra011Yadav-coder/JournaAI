package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final NewsFetchStateRepository newsFetchStateRepository;
  private final AuditService auditService;
  private final SearchRateLimiter searchRateLimiter;

  public ArticleService(ArticleRepository articleRepository,
                        NewsFetchStateRepository newsFetchStateRepository,
                        AuditService auditService,
                        SearchRateLimiter searchRateLimiter) {
    this.articleRepository = articleRepository;
    this.newsFetchStateRepository = newsFetchStateRepository;
    this.auditService = auditService;
    this.searchRateLimiter = searchRateLimiter;
  }

  public Page<Article> searchArticles(User actor,
                                      Long beatId,
                                      ArticleStatus status,
                                      Instant from,
                                      Instant to,
                                      int page,
                                      int size,
                                      int maxPerMinute) {
    searchRateLimiter.enforceSearchLimit(actor != null ? actor.getEmail() : null, maxPerMinute);
    auditService.record(actor, "SEARCH", "articles", null, beatId != null ? beatId.toString() : null);
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "providerPublishedAtUtc"));
    return articleRepository.findFiltered(beatId, status, from, to, pageable);
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

  public Optional<Instant> getLastRefreshedAt(Long beatId) {
    return newsFetchStateRepository.findByBeatId(beatId)
        .map(state -> state.getLastSuccessAt());
  }

  public Optional<Boolean> isStaleCache(Long beatId) {
    return newsFetchStateRepository.findByBeatId(beatId)
        .map(state -> state.getLastErrorCode() != null && !state.getLastErrorCode().isBlank());
  }
}
