package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.NewsFetchState;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final NewsFetchStateRepository newsFetchStateRepository;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;
    private final NewsProviderProperties newsProviderProperties;

    public ArticleService(ArticleRepository articleRepository,
                          NewsFetchStateRepository newsFetchStateRepository,
                          AuditService auditService,
                          RateLimiterService rateLimiterService,
                          NewsProviderProperties newsProviderProperties) {
        this.articleRepository = articleRepository;
        this.newsFetchStateRepository = newsFetchStateRepository;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
        this.newsProviderProperties = newsProviderProperties;
    }

    public Page<Article> searchArticles(String actor, String beat, String timeframe, Instant from, int page, int size) {
        rateLimiterService.enforceSearchLimit(actor, newsProviderProperties.getSearchesPerMinute());
        auditService.record(actor, "SEARCH", "articles", "{\"beat\":\"" + beat + "\"}");
        Instant fromTime = resolveFrom(timeframe, from);
        PageRequest pageable = PageRequest.of(page, size);
        if (beat == null || beat.isBlank()) {
            return articleRepository.findByPublishedAtAfterOrderByPublishedAtDesc(fromTime, pageable);
        }
        return articleRepository.findDistinctByBeats_NameIgnoreCaseAndPublishedAtAfterOrderByPublishedAtDesc(beat, fromTime, pageable);
    }

    public Article getArticle(Long id, String actor) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Article not found"));
        auditService.record(actor, "VIEW", "article", "{\"articleId\":" + id + "}");
        return article;
    }

    public Article saveArticle(Long id, boolean saved, String actor) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Article not found"));
        article.setSaved(saved);
        articleRepository.save(article);
        auditService.record(actor, saved ? "SAVE" : "UNSAVE", "article", "{\"articleId\":" + id + "}");
        return article;
    }

    public Optional<Instant> getLastRefresh(String beat, String timeframe) {
        return newsFetchStateRepository.findByBeat_NameIgnoreCaseAndTimeframe(beat, timeframe)
                .map(NewsFetchState::getLastFetchedAt);
    }

    private Instant resolveFrom(String timeframe, Instant from) {
        if (from != null) {
            return from;
        }
        Instant now = Instant.now();
        if ("7d".equalsIgnoreCase(timeframe)) {
            return now.minus(7, ChronoUnit.DAYS);
        }
        if ("30d".equalsIgnoreCase(timeframe)) {
            return now.minus(30, ChronoUnit.DAYS);
        }
        return now.minus(24, ChronoUnit.HOURS);
    }
}
