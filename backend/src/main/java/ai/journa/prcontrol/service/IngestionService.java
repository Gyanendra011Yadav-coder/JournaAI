package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.*;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatQueryRecipeRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import ai.journa.prcontrol.service.integration.FetchRequest;
import ai.journa.prcontrol.service.integration.FetchResult;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.ProviderArticle;
import ai.journa.prcontrol.service.integration.ProviderException;
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
  private final BeatRepository beatRepository;
  private final BeatQueryRecipeRepository recipeRepository;
  private final IntegrationSettingsService integrationSettingsService;
  private final NewsFetchStateRepository newsFetchStateRepository;
  private final ArticleRepository articleRepository;
  private final AuditService auditService;
  private final OutboundRateLimiter outboundRateLimiter;
  private final NewsProviderProperties properties;
  private final Map<ProviderType, NewsProvider> providers;

  public IngestionService(BeatRepository beatRepository,
                          BeatQueryRecipeRepository recipeRepository,
                          IntegrationSettingsService integrationSettingsService,
                          NewsFetchStateRepository newsFetchStateRepository,
                          ArticleRepository articleRepository,
                          AuditService auditService,
                          OutboundRateLimiter outboundRateLimiter,
                          NewsProviderProperties properties,
                          List<NewsProvider> providers) {
    this.beatRepository = beatRepository;
    this.recipeRepository = recipeRepository;
    this.integrationSettingsService = integrationSettingsService;
    this.newsFetchStateRepository = newsFetchStateRepository;
    this.articleRepository = articleRepository;
    this.auditService = auditService;
    this.outboundRateLimiter = outboundRateLimiter;
    this.properties = properties;
    this.providers = providers.stream().collect(Collectors.toMap(NewsProvider::type, provider -> provider));
  }

  @Transactional
  public RefreshResult refreshBeat(Long beatId, User actor, boolean respectTtl) {
    Beat beat = beatRepository.findById(beatId)
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    NewsFetchState state = newsFetchStateRepository.findByBeatId(beatId)
        .orElseGet(() -> createState(beat));

    Instant now = Instant.now();
    if (respectTtl && state.getLastSuccessAt() != null) {
      int ttlMinutes = settings.getTtlMinutes() != null ? settings.getTtlMinutes() : properties.getTtlMinutes();
      if (state.getLastSuccessAt().isAfter(now.minus(Duration.ofMinutes(ttlMinutes)))) {
        return RefreshResult.skipped("TTL not expired", state.getLastSuccessAt());
      }
    }

    if (isCircuitOpen(state, now)) {
      return RefreshResult.skipped("Circuit breaker open", state.getLastSuccessAt());
    }

    if (!settings.isEnabled()) {
      updateFailure(state, now, "DISABLED", "Integration disabled");
      auditService.record(actor, "INGEST_FAILED", "beat", Map.of("reason", "disabled"), beatId.toString());
      return RefreshResult.failed("Integration disabled", state.getLastSuccessAt());
    }

    String apiKey = integrationSettingsService.decryptApiKey(settings);
    if (apiKey == null || apiKey.isBlank()) {
      updateFailure(state, now, "MISSING_KEY", "API key not configured");
      auditService.record(actor, "INGEST_FAILED", "beat", Map.of("reason", "missing_key"), beatId.toString());
      return RefreshResult.failed("API key not configured", state.getLastSuccessAt());
    }

    BeatQueryRecipe recipe = recipeRepository.findByBeatId(beatId).stream().findFirst()
        .orElseThrow(() -> new IllegalStateException("Beat query recipe not configured"));

    FetchRequest fetchRequest = buildFetchRequest(recipe, settings);
    FetchResult result;
    try {
      result = fetchWithRetry(fetchRequest, settings.getProviderType(), apiKey);
      persistArticles(result.getArticles(), beat, settings.getProviderType());
      state.setLastSuccessAt(now);
      state.setLastAttemptAt(now);
      state.setLastErrorCode(null);
      state.setLastErrorMessage(null);
      state.setConsecutiveFailures(0);
      newsFetchStateRepository.save(state);
      auditService.record(actor, "INGEST_SUCCESS", "beat", Map.of("articles", result.getArticles().size()), beatId.toString());
      return RefreshResult.success(state.getLastSuccessAt());
    } catch (ProviderException ex) {
      updateFailure(state, now, String.valueOf(ex.getStatusCode()), ex.getMessage());
      auditService.record(actor, "INGEST_FAILED", "beat", Map.of("status", ex.getStatusCode()), beatId.toString());
      return RefreshResult.failed(ex.getMessage(), state.getLastSuccessAt());
    } catch (Exception ex) {
      updateFailure(state, now, "UNKNOWN", "Unexpected ingestion error");
      auditService.record(actor, "INGEST_FAILED", "beat", Map.of("reason", "unexpected"), beatId.toString());
      return RefreshResult.failed("Unexpected ingestion error", state.getLastSuccessAt());
    }
  }

  FetchRequest buildFetchRequest(BeatQueryRecipe recipe, IntegrationSettings settings) {
    FetchRequest request = new FetchRequest();
    request.setEndpointType(recipe.getEndpointType());
    request.setQuery(recipe.getQuery());
    request.setCategory(recipe.getCategory());
    request.setLang(recipe.getLang() != null ? recipe.getLang() : settings.getDefaultLang());
    request.setCountry(recipe.getCountry() != null ? recipe.getCountry() : settings.getDefaultCountry());
    request.setInFields(recipe.getInFields());
    request.setNullableFields(recipe.getNullableFields());
    request.setMax(recipe.getMax() != null ? recipe.getMax() : settings.getMaxPerRequest());
    request.setSort(recipe.getSort());
    return request;
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
        outboundRateLimiter.acquire();
        return provider.fetch(request, apiKey);
      } catch (ProviderException ex) {
        if (!ex.isRetryable() || attempts >= maxAttempts) {
          throw ex;
        }
        sleepBackoff(backoffSeconds);
        backoffSeconds = Math.min(backoffSeconds * 2, properties.getGnews().getMaxBackoffSeconds());
      }
    }
  }

  private void persistArticles(List<ProviderArticle> articles, Beat beat, ProviderType providerType) {
    for (ProviderArticle article : articles) {
      if (article.getUrl() == null || article.getUrl().isBlank()) {
        continue;
      }
      if (articleRepository.findByUrl(article.getUrl()).isPresent()) {
        continue;
      }
      Article entity = new Article();
      entity.setProviderType(providerType);
      entity.setProviderArticleId(article.getId());
      entity.setBeat(beat);
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
      try {
        articleRepository.save(entity);
      } catch (DataIntegrityViolationException ignored) {
        // dedupe conflicts
      }
    }
  }

  private NewsFetchState createState(Beat beat) {
    NewsFetchState state = new NewsFetchState();
    state.setBeat(beat);
    state.setConsecutiveFailures(0);
    return newsFetchStateRepository.save(state);
  }

  private void updateFailure(NewsFetchState state, Instant now, String code, String message) {
    state.setLastAttemptAt(now);
    state.setLastErrorCode(code);
    state.setLastErrorMessage(message);
    state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
    newsFetchStateRepository.save(state);
  }

  private boolean isCircuitOpen(NewsFetchState state, Instant now) {
    if (state.getConsecutiveFailures() < properties.getFailureThreshold()) {
      return false;
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
}
