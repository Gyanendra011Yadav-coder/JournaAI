package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.NewsFetchState;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.dto.RefreshRequest;
import ai.journa.prcontrol.dto.RefreshResponse;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.manual.ManualArticleProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.integration.model.NewsFetchRequest;
import ai.journa.prcontrol.service.summarizer.Summarizer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NewsIngestionService {
    private final NewsProvider gNewsProvider;
    private final NewsProvider rssNewsProvider;
    private final ArticleRepository articleRepository;
    private final BeatRepository beatRepository;
    private final NewsFetchStateRepository newsFetchStateRepository;
    private final NewsProviderProperties properties;
    private final Summarizer summarizer;
    private final AuditService auditService;
    private final ManualArticleProvider manualArticleProvider = new ManualArticleProvider();

    public NewsIngestionService(@org.springframework.beans.factory.annotation.Qualifier("gNewsProvider") NewsProvider gNewsProvider,
                                @org.springframework.beans.factory.annotation.Qualifier("rssNewsProvider") NewsProvider rssNewsProvider,
                                ArticleRepository articleRepository,
                                BeatRepository beatRepository,
                                NewsFetchStateRepository newsFetchStateRepository,
                                NewsProviderProperties properties,
                                Summarizer summarizer,
                                AuditService auditService) {
        this.gNewsProvider = gNewsProvider;
        this.rssNewsProvider = rssNewsProvider;
        this.articleRepository = articleRepository;
        this.beatRepository = beatRepository;
        this.newsFetchStateRepository = newsFetchStateRepository;
        this.properties = properties;
        this.summarizer = summarizer;
        this.auditService = auditService;
    }

    public RefreshResponse refresh(String actor, RefreshRequest request) {
        Beat beat = getBeat(request.getBeat());
        NewsFetchState state = newsFetchStateRepository
                .findByBeat_NameIgnoreCaseAndTimeframe(request.getBeat(), request.getTimeframe())
                .orElseGet(() -> createState(beat, request.getTimeframe()));

        Instant now = Instant.now();
        if (state.getLastFetchedAt() != null && state.getLastFetchedAt().plusSeconds(properties.getTtlMinutes() * 60L).isAfter(now)) {
            return buildResponse("rate_limited", false, state.getLastFetchedAt(), "Refresh is rate-limited by TTL.");
        }
        if (state.getCircuitOpenUntil() != null && state.getCircuitOpenUntil().isAfter(now)) {
            return buildResponse("circuit_open", true, state.getLastFetchedAt(), "Circuit breaker open. Serving cached data.");
        }

        NewsFetchRequest fetchRequest = new NewsFetchRequest();
        fetchRequest.setBeat(request.getBeat());
        fetchRequest.setTimeframe(request.getTimeframe());
        fetchRequest.setFrom(request.getFrom());
        fetchRequest.setTo(request.getTo());
        fetchRequest.setPage(0);
        fetchRequest.setSize(50);

        try {
            List<NewsArticle> results = gNewsProvider.fetchArticles(fetchRequest);
            saveArticles(results, beat);
            resetState(state, now);
            auditService.record(actor, "REFRESH", "articles", "{\"beat\":\"" + request.getBeat() + "\"}");
            return buildResponse("fresh", false, state.getLastFetchedAt(), "GNews refresh completed.");
        } catch (RuntimeException ex) {
            updateFailure(state, now);
            try {
                List<NewsArticle> rssResults = rssNewsProvider.fetchArticles(fetchRequest);
                saveArticles(rssResults, beat);
                state.setLastFetchedAt(now);
                newsFetchStateRepository.save(state);
                auditService.record(actor, "REFRESH", "articles", "{\"beat\":\"" + request.getBeat() + "\",\"fallback\":\"rss\"}");
                return buildResponse("rss_fallback", false, state.getLastFetchedAt(), "GNews failed; RSS fallback succeeded.");
            } catch (RuntimeException rssEx) {
                newsFetchStateRepository.save(state);
                return buildResponse("stale_cache", true, state.getLastFetchedAt(), "Refresh failed. Serving cached data.");
            }
        }
    }

    public Article addManualArticle(String actor, ManualArticleRequest request) {
        Beat beat = getBeat(request.getBeat());
        NewsArticle manual = manualArticleProvider.createArticle(request);
        Article article = saveArticle(manual, beat);
        auditService.record(actor, "ADD", "article", "{\"articleId\":" + article.getId() + "}");
        return article;
    }

    private Beat getBeat(String beatName) {
        return beatRepository.findAll().stream()
                .filter(beat -> beat.getName().equalsIgnoreCase(beatName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Beat not found"));
    }

    private NewsFetchState createState(Beat beat, String timeframe) {
        NewsFetchState state = new NewsFetchState();
        state.setBeat(beat);
        state.setTimeframe(timeframe);
        return newsFetchStateRepository.save(state);
    }

    private void resetState(NewsFetchState state, Instant now) {
        state.setLastFetchedAt(now);
        state.setFailureCount(0);
        state.setLastFailureAt(null);
        state.setCircuitOpenUntil(null);
        newsFetchStateRepository.save(state);
    }

    private void updateFailure(NewsFetchState state, Instant now) {
        state.setFailureCount(state.getFailureCount() + 1);
        state.setLastFailureAt(now);
        if (state.getFailureCount() >= properties.getFailureThreshold()) {
            state.setCircuitOpenUntil(now.plusSeconds(properties.getCircuitMinutes() * 60L));
        }
    }

    private void saveArticles(List<NewsArticle> results, Beat beat) {
        for (NewsArticle result : results) {
            saveArticle(result, beat);
        }
    }

    private Article saveArticle(NewsArticle result, Beat beat) {
        Optional<Article> existing = articleRepository.findByCanonicalUrl(result.getCanonicalUrl());
        Article article = existing.orElseGet(Article::new);
        article.setHeadline(result.getHeadline());
        article.setSource(result.getSource());
        article.setAuthor(result.getAuthor());
        article.setCanonicalUrl(result.getCanonicalUrl());
        article.setUrl(result.getUrl());
        article.setPublishedAt(result.getPublishedAt());
        String summaryInput = result.getDescription() != null ? result.getDescription() : result.getHeadline();
        article.setSummary(summarizer.summarize(summaryInput));
        article.setProvider(result.getProvider());
        article.setRawPayload(result.getRawPayload());
        article.getBeats().add(beat);
        return articleRepository.save(article);
    }

    private RefreshResponse buildResponse(String status, boolean staleCache, Instant lastRefreshedAt, String message) {
        RefreshResponse response = new RefreshResponse();
        response.setStatus(status);
        response.setStaleCache(staleCache);
        response.setLastRefreshedAt(lastRefreshedAt);
        response.setMessage(message);
        return response;
    }
}
