package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.SavedSearch;
import ai.journa.prcontrol.dto.ArticleSearchRequest;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.SavedSearchRepository;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.summarizer.Summarizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final NewsProvider newsProvider;
    private final ArticleRepository articleRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final BeatRepository beatRepository;
    private final Summarizer summarizer;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public ArticleService(NewsProvider newsProvider,
                          ArticleRepository articleRepository,
                          SavedSearchRepository savedSearchRepository,
                          BeatRepository beatRepository,
                          Summarizer summarizer,
                          AuditService auditService,
                          RateLimiterService rateLimiterService) {
        this.newsProvider = newsProvider;
        this.articleRepository = articleRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.beatRepository = beatRepository;
        this.summarizer = summarizer;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    public List<Article> searchArticles(String actor, ArticleSearchRequest request) {
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setTimeframe(request.getTimeframe());
        savedSearch.setFilters(request.getFilters());
        beatRepository.findAll().stream()
                .filter(beat -> beat.getName().equalsIgnoreCase(request.getBeat()))
                .findFirst()
                .ifPresent(savedSearch::setBeat);
        savedSearchRepository.save(savedSearch);

        auditService.record(actor, "SEARCH", "articles", "{\"beat\":\"" + request.getBeat() + "\"}");

        List<NewsArticle> results = withRetry(() -> {
            rateLimiterService.throttle(300);
            return newsProvider.fetchArticles(request.getBeat(), request.getTimeframe(), request.getFilters(), request.getPage());
        });
        List<Article> articles = results.stream().map(result -> {
            Article article = new Article();
            article.setHeadline(result.getHeadline());
            article.setSource(result.getSource());
            article.setAuthor(result.getAuthor());
            article.setUrl(result.getUrl());
            article.setPublishedAt(result.getPublishedAt());
            article.setRawPayload(result.getRawPayload());
            String summaryInput = result.getDescription() != null ? result.getDescription() : result.getHeadline();
            article.setSummary(summarizer.summarize(summaryInput));
            return articleRepository.save(article);
        }).collect(Collectors.toList());

        return articles;
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

    private List<NewsArticle> withRetry(NewsCall call) {
        int attempts = 0;
        RuntimeException last = null;
        while (attempts < 3) {
            try {
                return call.get();
            } catch (RuntimeException ex) {
                last = ex;
                attempts++;
            }
        }
        throw last != null ? last : new IllegalStateException("Unable to fetch articles");
    }

    @FunctionalInterface
    private interface NewsCall {
        List<NewsArticle> get();
    }
}
