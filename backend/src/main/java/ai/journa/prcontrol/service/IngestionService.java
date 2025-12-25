package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.*;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import ai.journa.prcontrol.repository.NewsCacheRepository;
import ai.journa.prcontrol.service.integration.FetchRequest;
import ai.journa.prcontrol.service.integration.FetchResult;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.ProviderArticle;
import ai.journa.prcontrol.service.integration.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IngestionService {
  private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

  private final BeatRepository beatRepository;
  private final IntegrationSettingsService integrationSettingsService;
  private final NewsFetchStateRepository newsFetchStateRepository;
  private final NewsCacheRepository newsCacheRepository;
  private final ArticleRepository articleRepository;
  private final AuditService auditService;
  private final OutboundRateLimiter outboundRateLimiter;
  private final NewsProviderProperties properties;
  private final Map<ProviderType, NewsProvider> providers;
  private final QueryBuilderService queryBuilderService;
  private final CacheKeyService cacheKeyService;

  public IngestionService(BeatRepository beatRepository,
                          IntegrationSettingsService integrationSettingsService,
                          NewsFetchStateRepository newsFetchStateRepository,
                          NewsCacheRepository newsCacheRepository,
                          ArticleRepository articleRepository,
                          AuditService auditService,
                          OutboundRateLimiter outboundRateLimiter,
                          NewsProviderProperties properties,
                          List<NewsProvider> providers,
                          QueryBuilderService queryBuilderService,
                          CacheKeyService cacheKeyService) {
    this.beatRepository = beatRepository;
    this.integrationSettingsService = integrationSettingsService;
    this.newsFetchStateRepository = newsFetchStateRepository;
    this.newsCacheRepository = newsCacheRepository;
    this.articleRepository = articleRepository;
    this.auditService = auditService;
    this.outboundRateLimiter = outboundRateLimiter;
    this.properties = properties;
    this.providers = providers.stream().collect(Collectors.toMap(NewsProvider::type, provider -> provider));
    this.queryBuilderService = queryBuilderService;
    this.cacheKeyService = cacheKeyService;
  }

  @Transactional
  public RefreshResult refresh(IngestRequest request, LocaleResolver.Resolution locale, User actor, boolean respectTtl) {
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    Instant now = Instant.now();
    FetchRequest fetchRequest = buildFetchRequest(request, locale, settings);
    String cacheKey = cacheKeyService.build(
        request.getMode(),
        request.getLensOrTrack(),
        request.getBeatId(),
        request.getCategory(),
        fetchRequest.getQuery(),
        locale
    );
    NewsFetchState state = newsFetchStateRepository.findByKey(cacheKey)
        .orElseGet(() -> createState(cacheKey, request));

    logger.info("Ingest start mode={} lensOrTrack={} actor={} cacheKey={}",
        request.getMode(),
        request.getLensOrTrack(),
        actor != null ? actor.getEmail() : "anonymous",
        cacheKey);

    NewsCache cached = newsCacheRepository.findByCacheKey(cacheKey).orElse(null);
    if (respectTtl && cached != null && cached.getExpiresAtUtc() != null && cached.getExpiresAtUtc().isAfter(now)) {
      return RefreshResult.skipped("TTL not expired", cached.getLastSuccessAtUtc());
    }

    if (isCircuitOpen(state, now)) {
      return RefreshResult.skipped("Circuit breaker open", state.getLastSuccessAt());
    }

    if (!settings.isEnabled()) {
      updateFailure(state, now, "DISABLED", "Integration disabled");
      auditService.record(actor, "INGEST_FAILED", "news", Map.of("reason", "disabled"), cacheKey);
      return RefreshResult.failed("Integration disabled", state.getLastSuccessAt());
    }

    String apiKey = integrationSettingsService.resolveApiKey(settings);
    if (apiKey == null || apiKey.isBlank()) {
      updateFailure(state, now, "MISSING_KEY", "API key not configured");
      auditService.record(actor, "INGEST_FAILED", "news", Map.of("reason", "missing_key"), cacheKey);
      return RefreshResult.failed("API key not configured", state.getLastSuccessAt());
    }

    FetchResult result;
    try {
      result = fetchWithRetry(fetchRequest, settings.getProviderType(), apiKey);
      int savedCount = persistArticles(result.getArticles(), request, fetchRequest, settings.getProviderType());
      state.setLastSuccessAt(now);
      state.setLastAttemptAt(now);
      state.setLastErrorCode(null);
      state.setLastErrorMessage(null);
      state.setConsecutiveFailures(0);
      newsFetchStateRepository.save(state);
      upsertCache(cacheKey, now, settings, result);
      auditService.record(actor, "INGEST_SUCCESS", "news", Map.of("articles", result.getArticles().size()), cacheKey);
      logger.info("Ingest success cacheKey={} fetched={} saved={}", cacheKey, result.getArticles().size(), savedCount);
      return RefreshResult.success(state.getLastSuccessAt());
    } catch (ProviderException ex) {
      updateFailure(state, now, String.valueOf(ex.getStatusCode()), ex.getMessage());
      updateCacheFailure(cacheKey, now, ex.getStatusCode(), ex.getMessage());
      auditService.record(actor, "INGEST_FAILED", "news", Map.of("status", ex.getStatusCode()), cacheKey);
      logger.warn("Ingest failed cacheKey={} status={} message={}", cacheKey, ex.getStatusCode(), ex.getMessage());
      return RefreshResult.failed(ex.getMessage(), state.getLastSuccessAt());
    } catch (Exception ex) {
      updateFailure(state, now, "UNKNOWN", "Unexpected ingestion error");
      updateCacheFailure(cacheKey, now, 500, "Unexpected ingestion error");
      auditService.record(actor, "INGEST_FAILED", "news", Map.of("reason", "unexpected"), cacheKey);
      logger.error("Ingest failed cacheKey={} reason=UNEXPECTED", cacheKey, ex);
      return RefreshResult.failed("Unexpected ingestion error", state.getLastSuccessAt());
    }
  }

  private FetchRequest buildFetchRequest(IngestRequest request, LocaleResolver.Resolution locale, IntegrationSettings settings) {
    FetchRequest fetchRequest = new FetchRequest();
    if (request.getMode() == IngestMode.SEARCH) {
      Beat beat = beatRepository.findById(request.getBeatId())
          .orElseThrow(() -> new IllegalStateException("Beat not found"));
      QueryBuilderService.QueryBundle bundle = queryBuilderService.buildSearchQueries(beat, request.getActor());
      fetchRequest.setEndpointType(EndpointType.SEARCH);
      fetchRequest.setQuery(request.getLensOrTrack().equalsIgnoreCase("CLIENT") ? bundle.clientQuery() : bundle.beatQuery());
      fetchRequest.setLang(locale.lang());
      fetchRequest.setCountry(locale.country());
      fetchRequest.setInFields(bundle.template().getInDefault());
      fetchRequest.setNullableFields(bundle.template().getNullableFields());
      fetchRequest.setMax(bundle.template().getMaxDefault() != null ? bundle.template().getMaxDefault() : settings.getMaxPerRequest());
      fetchRequest.setSort(bundle.template().getSortbyDefault());
    } else {
      fetchRequest.setEndpointType(EndpointType.TOP_HEADLINES);
      fetchRequest.setCategory(request.getCategory() != null ? request.getCategory() : "general");
      fetchRequest.setQuery(request.getQuery());
      fetchRequest.setLang(locale.lang());
      if ("LOCAL".equalsIgnoreCase(request.getLensOrTrack())) {
        fetchRequest.setCountry(locale.country());
      }
      fetchRequest.setMax(settings.getMaxPerRequest());
    }
    return fetchRequest;
  }

  private FetchResult fetchWithRetry(FetchRequest request, ProviderType providerType, String apiKey) {
    NewsProvider provider = providers.get(providerType);
    if (provider == null) {
      throw new ProviderException("Provider not supported", 400, false);
    }
    int attempts = 0;
    int maxAttempts = 3;
    long backoffSeconds = 1;
    while (true) {
      attempts++;
      try {
        logger.info("Ingest provider call provider={} attempt={} query={}",
            providerType,
            attempts,
            request.getQuery());
        outboundRateLimiter.acquire();
        return provider.fetch(request, apiKey);
      } catch (ProviderException ex) {
        logger.warn("Ingest provider error provider={} attempt={} status={} retryable={}",
            providerType,
            attempts,
            ex.getStatusCode(),
            ex.isRetryable());
        if (!ex.isRetryable() || attempts >= maxAttempts) {
          throw ex;
        }
        sleepBackoff(backoffSeconds);
        backoffSeconds = Math.min(backoffSeconds * 2, properties.getGnews().getMaxBackoffSeconds());
      }
    }
  }

  private int persistArticles(List<ProviderArticle> articles, IngestRequest request, FetchRequest fetchRequest, ProviderType providerType) {
    int saved = 0;
    for (ProviderArticle article : articles) {
      if (article.getUrl() == null || article.getUrl().isBlank()) {
        continue;
      }
      Article entity = articleRepository.findByUrl(article.getUrl()).orElse(null);
      if (entity == null) {
        entity = new Article();
        entity.setProviderType(providerType);
        entity.setProviderArticleId(article.getId());
        if (request.getBeatId() != null) {
          Beat beat = beatRepository.findById(request.getBeatId())
              .orElseThrow(() -> new IllegalStateException("Beat not found"));
          entity.setBeat(beat);
        }
        entity.setCategory(fetchRequest.getCategory());
        entity.setLensSource(request.getLensSource());
        entity.setClientMatch(request.getLensSource() == LensSource.CLIENT);
        entity.setTitle(article.getTitle());
        entity.setDescription(article.getDescription());
        entity.setContent(article.getContent());
        entity.setUrl(article.getUrl());
        entity.setImageUrl(article.getImage());
        entity.setProviderPublishedAtUtc(article.getPublishedAt());
        entity.setLang(article.getLang());
        entity.setSourceId(article.getSourceId());
        entity.setSourceName(article.getSourceName());
        entity.setSourceUrl(article.getSourceUrl());
        entity.setSourceCountry(article.getSourceCountry());
        entity.setRawPayloadJsonb(article.getRawPayload());
        entity.setStatus(ArticleStatus.INGESTED);
      } else if (request.getLensSource() == LensSource.CLIENT && !entity.isClientMatch()) {
        entity.setClientMatch(true);
      }
      try {
        articleRepository.save(entity);
        saved++;
      } catch (DataIntegrityViolationException ignored) {
      }
    }
    return saved;
  }

  private NewsFetchState createState(String cacheKey, IngestRequest request) {
    NewsFetchState state = new NewsFetchState();
    state.setKey(cacheKey);
    state.setMode(request.getMode().name());
    state.setLensOrTrack(request.getLensOrTrack());
    if (request.getBeatId() != null) {
      Beat beat = beatRepository.findById(request.getBeatId())
          .orElseThrow(() -> new IllegalStateException("Beat not found"));
      state.setBeat(beat);
    }
    state.setCategory(request.getCategory());
    state.setConsecutiveFailures(0);
    return newsFetchStateRepository.save(state);
  }

  private void updateFailure(NewsFetchState state, Instant now, String code, String message) {
    state.setLastAttemptAt(now);
    state.setLastErrorCode(code);
    state.setLastErrorMessage(message);
    state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
    if (state.getConsecutiveFailures() >= properties.getFailureThreshold()) {
      state.setCooldownUntil(now.plus(Duration.ofMinutes(properties.getCircuitMinutes())));
    }
    newsFetchStateRepository.save(state);
  }

  private boolean isCircuitOpen(NewsFetchState state, Instant now) {
    if (state.getConsecutiveFailures() < properties.getFailureThreshold()) {
      return false;
    }
    if (state.getCooldownUntil() != null && state.getCooldownUntil().isAfter(now)) {
      return true;
    }
    Instant lastAttempt = state.getLastAttemptAt();
    if (lastAttempt == null) {
      return false;
    }
    return lastAttempt.isAfter(now.minus(Duration.ofMinutes(properties.getCircuitMinutes())));
  }

  private void sleepBackoff(long backoffSeconds) {
    try {
      Thread.sleep(Duration.ofSeconds(backoffSeconds).toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void upsertCache(String cacheKey, Instant now, IntegrationSettings settings, FetchResult result) {
    NewsCache cache = newsCacheRepository.findByCacheKey(cacheKey).orElseGet(NewsCache::new);
    cache.setCacheKey(cacheKey);
    cache.setPayloadJsonb(result.getRawPayload());
    cache.setFetchedAtUtc(now);
    cache.setLastSuccessAtUtc(now);
    int ttlMinutes = settings.getTtlMinutes() != null ? settings.getTtlMinutes() : properties.getTtlMinutes();
    cache.setExpiresAtUtc(now.plus(Duration.ofMinutes(ttlMinutes)));
    cache.setLastErrorCode(null);
    cache.setLastErrorMessage(null);
    newsCacheRepository.save(cache);
  }

  private void updateCacheFailure(String cacheKey, Instant now, int statusCode, String message) {
    NewsCache cache = newsCacheRepository.findByCacheKey(cacheKey).orElseGet(NewsCache::new);
    cache.setCacheKey(cacheKey);
    cache.setFetchedAtUtc(now);
    cache.setExpiresAtUtc(now);
    cache.setLastErrorCode(String.valueOf(statusCode));
    cache.setLastErrorMessage(message);
    newsCacheRepository.save(cache);
  }

  public static class RefreshResult {
    private final boolean success;
    private final boolean skipped;
    private final String message;
    private final Instant lastSuccessAt;

    private RefreshResult(boolean success, boolean skipped, String message, Instant lastSuccessAt) {
      this.success = success;
      this.skipped = skipped;
      this.message = message;
      this.lastSuccessAt = lastSuccessAt;
    }

    public static RefreshResult success(Instant lastSuccessAt) {
      return new RefreshResult(true, false, null, lastSuccessAt);
    }

    public static RefreshResult skipped(String message, Instant lastSuccessAt) {
      return new RefreshResult(false, true, message, lastSuccessAt);
    }

    public static RefreshResult failed(String message, Instant lastSuccessAt) {
      return new RefreshResult(false, false, message, lastSuccessAt);
    }

    public boolean isSuccess() {
      return success;
    }

    public boolean isSkipped() {
      return skipped;
    }

    public String getMessage() {
      return message;
    }

    public Instant getLastSuccessAt() {
      return lastSuccessAt;
    }
  }

  public static class IngestRequest {
    private final IngestMode mode;
    private final Long beatId;
    private final String category;
    private final String lensOrTrack;
    private final String query;
    private final User actor;

    public IngestRequest(IngestMode mode, Long beatId, String category, String lensOrTrack, String query, User actor) {
      this.mode = mode;
      this.beatId = beatId;
      this.category = category;
      this.lensOrTrack = lensOrTrack;
      this.query = query;
      this.actor = actor;
    }

    public IngestMode getMode() {
      return mode;
    }

    public Long getBeatId() {
      return beatId;
    }

    public String getCategory() {
      return category;
    }

    public String getLensOrTrack() {
      return lensOrTrack;
    }

    public String getQuery() {
      return query;
    }

    public User getActor() {
      return actor;
    }

    public LensSource getLensSource() {
      if (mode == IngestMode.SEARCH) {
        return "CLIENT".equalsIgnoreCase(lensOrTrack) ? LensSource.CLIENT : LensSource.BEAT;
      }
      return "LOCAL".equalsIgnoreCase(lensOrTrack) ? LensSource.TRENDING_LOCAL : LensSource.TRENDING_GLOBAL;
    }
  }
}
