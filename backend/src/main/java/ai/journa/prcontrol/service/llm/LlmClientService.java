package ai.journa.prcontrol.service.llm;

import ai.journa.prcontrol.config.LlmProperties;
import ai.journa.prcontrol.domain.LlmAuthType;
import ai.journa.prcontrol.domain.LlmProvider;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.repository.LlmProviderRepository;
import ai.journa.prcontrol.service.AuditService;
import ai.journa.prcontrol.service.EncryptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LlmClientService {
  private static final Logger logger = LoggerFactory.getLogger(LlmClientService.class);

  private final LlmProviderRepository llmProviderRepository;
  private final EncryptionService encryptionService;
  private final AuditService auditService;
  private final LlmProperties llmProperties;
  private final LlmTemplateRenderer templateRenderer;
  private final ObjectMapper objectMapper;
  private final ConcurrentMap<String, CircuitState> circuitStateByProvider = new ConcurrentHashMap<>();

  public LlmClientService(LlmProviderRepository llmProviderRepository,
                          EncryptionService encryptionService,
                          AuditService auditService,
                          LlmProperties llmProperties,
                          LlmTemplateRenderer templateRenderer,
                          ObjectMapper objectMapper) {
    this.llmProviderRepository = llmProviderRepository;
    this.encryptionService = encryptionService;
    this.auditService = auditService;
    this.llmProperties = llmProperties;
    this.templateRenderer = templateRenderer;
    this.objectMapper = objectMapper;
  }

  public Optional<LlmResponse> generate(LlmRequest request, User actor) {
    return generateValidated(request, actor, null);
  }

  public Optional<LlmResponse> generateValidated(LlmRequest request, User actor,
                                                 java.util.function.Predicate<String> validator) {
    if (!llmProperties.isEnabled()) {
      return Optional.empty();
    }

    List<LlmProvider> providers = orderedProviders();
    for (LlmProvider provider : providers) {
      if (!provider.isEnabled()) {
        continue;
      }
      if (isCircuitOpen(provider.getName())) {
        continue;
      }
      try {
        LlmResponse response = callProvider(provider, request, actor, validator);
        markSuccess(provider.getName());
        return Optional.of(response);
      } catch (LlmCallException ex) {
        markFailure(provider.getName());
        if (logger.isWarnEnabled()) {
          logger.warn("LLM provider {} failed status={} retryable={} message={}",
              provider.getName(), ex.getStatusCode(), ex.isRetryable(), ex.getMessage());
        }
      }
    }
    return Optional.empty();
  }

  private List<LlmProvider> orderedProviders() {
    List<LlmProvider> providers = llmProviderRepository.findAll().stream()
        .filter(LlmProvider::isEnabled)
        .filter(provider -> !isGeminiProvider(provider))
        .toList();
    List<String> order = llmProperties.getProviderOrder();
    if (order == null || order.isEmpty()) {
      return providers;
    }
    Map<String, Integer> orderIndex = new LinkedHashMap<>();
    for (int index = 0; index < order.size(); index++) {
      orderIndex.put(normalize(order.get(index)), index);
    }
    return providers.stream()
        .sorted(Comparator.comparingInt(provider -> orderIndex.getOrDefault(normalize(provider.getName()), Integer.MAX_VALUE)))
        .toList();
  }

  private LlmResponse callProvider(LlmProvider provider, LlmRequest request, User actor,
                                   java.util.function.Predicate<String> validator) {
    Instant start = Instant.now();
    RetryPolicy retryPolicy = parseRetryPolicy(provider.getRetryPolicyJsonb());
    int attempts = 0;
    LlmCallException lastException = null;
    boolean attemptedGeminiResolve = false;

    while (attempts <= retryPolicy.maxRetries()) {
      attempts++;
      try {
        String content = executeCall(provider, request);
        if (validator != null && !validator.test(content)) {
          logger.warn("LLM provider {} response failed validation content={}",
              provider.getName(),
              truncateBody(content, 4000));
          throw new LlmCallException("LLM response failed validation", null, false, null);
        }
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        recordAudit(actor, provider, request, durationMs, true, null);
        return new LlmResponse(provider.getName(), provider.getModel(), content, durationMs);
      } catch (LlmCallException ex) {
        lastException = ex;
        recordAudit(actor, provider, request, Duration.between(start, Instant.now()).toMillis(), false, ex);
        if (!ex.isRetryable() || attempts > retryPolicy.maxRetries()) {
          throw ex;
        }
      } catch (RestClientResponseException ex) {
        boolean retryable = retryPolicy.retryableStatusCodes().contains(ex.getRawStatusCode());
        String errorBody = truncateBody(ex.getResponseBodyAsString(), 1200);
        if (logger.isWarnEnabled() && errorBody != null) {
          logger.warn("LLM provider {} error body={}", provider.getName(), errorBody);
        }
        if (!attemptedGeminiResolve && ex.getRawStatusCode() == 404 && isGeminiProvider(provider)
            && isGeminiModelNotFound(errorBody)) {
          Optional<GeminiModelResolution> resolved = resolveGeminiModel(provider);
          if (resolved.isPresent()) {
            applyGeminiResolution(provider, resolved.get());
            attemptedGeminiResolve = true;
            attempts--;
            continue;
          }
        }
        lastException = new LlmCallException("LLM provider returned error", ex.getRawStatusCode(), retryable, ex);
        recordAudit(actor, provider, request, Duration.between(start, Instant.now()).toMillis(), false, ex);
        if (!retryable || attempts > retryPolicy.maxRetries()) {
          throw lastException;
        }
      } catch (ResourceAccessException ex) {
        lastException = new LlmCallException("LLM provider network error", null, true, ex);
        recordAudit(actor, provider, request, Duration.between(start, Instant.now()).toMillis(), false, ex);
        if (attempts > retryPolicy.maxRetries()) {
          throw lastException;
        }
      } catch (Exception ex) {
        lastException = new LlmCallException("LLM provider failed", null, false, ex);
        recordAudit(actor, provider, request, Duration.between(start, Instant.now()).toMillis(), false, ex);
        throw lastException;
      }
    }
    throw lastException == null
        ? new LlmCallException("LLM provider failed with no response", null, false, null)
        : lastException;
  }

  private String executeCall(LlmProvider provider, LlmRequest request) {
    String apiKey = encryptionService.decrypt(provider.getApiKeyEncrypted());
    String renderedBaseUrl = templateRenderer.renderBaseUrl(provider.getBaseUrl(), provider);
    Object template = readJson(provider.getRequestTemplateJsonb(), new TypeReference<Map<String, Object>>() {});
    Object rendered = templateRenderer.renderTemplate(template, provider, request);
    String body = writeJson(rendered);

    URI uri = buildUri(renderedBaseUrl, provider, apiKey);
    logRequest(provider, uri, body);
    RestClient restClient = buildRestClient(provider.getTimeoutMs());

    RestClient.RequestBodySpec spec = restClient.post()
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body);

    spec = applyAuth(spec, provider, apiKey);
    String response = spec.retrieve().body(String.class);
    logResponse(provider, response);
    return extractContent(response, provider.getResponseJsonpath());
  }

  private URI buildUri(String baseUrl, LlmProvider provider, String apiKey) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
    if (provider.getAuthType() == LlmAuthType.QUERY_PARAM && apiKey != null && !apiKey.isBlank()) {
      String paramName = provider.getAuthQueryParamName();
      if (paramName == null || paramName.isBlank()) {
        paramName = "key";
      }
      builder.queryParam(paramName, apiKey);
    }
    return builder.build(true).toUri();
  }

  private RestClient.RequestBodySpec applyAuth(RestClient.RequestBodySpec spec, LlmProvider provider, String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      return spec;
    }
    return spec.headers(headers -> applyAuthHeaders(headers, provider, apiKey));
  }

  private void applyAuthHeaders(HttpHeaders headers, LlmProvider provider, String apiKey) {
    if (provider.getAuthType() == LlmAuthType.BEARER) {
      headers.setBearerAuth(apiKey);
    } else if (provider.getAuthType() == LlmAuthType.API_KEY_HEADER) {
      String header = provider.getAuthHeaderName();
      if (header == null || header.isBlank()) {
        header = "X-Api-Key";
      }
      headers.set(header, apiKey);
    }
  }

  private RestClient buildRestClient(int timeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    return RestClient.builder().requestFactory(factory).build();
  }

  private String extractContent(String responseBody, String jsonPath) {
    if (responseBody == null || responseBody.isBlank()) {
      throw new LlmCallException("LLM provider returned empty response", null, false, null);
    }
    try {
      Object result = JsonPath.read(responseBody, jsonPath);
      if (result == null) {
        throw new LlmCallException("LLM provider response missing content", null, false, null);
      }
      String content = String.valueOf(result).trim();
      if (content.isBlank()) {
        throw new LlmCallException("LLM provider response empty content", null, false, null);
      }
      return content;
    } catch (Exception ex) {
      throw new LlmCallException("Failed to parse LLM response", null, false, ex);
    }
  }

  private boolean isCircuitOpen(String providerName) {
    CircuitState state = circuitStateByProvider.get(providerName);
    if (state == null) {
      return false;
    }
    if (state.openUntil == null) {
      return false;
    }
    if (Instant.now().isBefore(state.openUntil)) {
      return true;
    }
    return false;
  }

  private void markFailure(String providerName) {
    CircuitState state = circuitStateByProvider.computeIfAbsent(providerName, key -> new CircuitState());
    state.failures++;
    if (state.failures >= llmProperties.getCircuitBreaker().getFailureThreshold()) {
      state.openUntil = Instant.now().plus(Duration.ofMinutes(llmProperties.getCircuitBreaker().getOpenMinutes()));
      state.failures = 0;
    }
  }

  private void markSuccess(String providerName) {
    CircuitState state = circuitStateByProvider.computeIfAbsent(providerName, key -> new CircuitState());
    state.failures = 0;
    state.openUntil = null;
  }

  private RetryPolicy parseRetryPolicy(String json) {
    if (json == null || json.isBlank()) {
      return RetryPolicy.defaults();
    }
    try {
      Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
      int maxRetries = parseInt(map.get("maxRetries"), RetryPolicy.defaults().maxRetries());
      List<Integer> codes = parseIntList(map.get("retryableStatusCodes"));
      if (codes.isEmpty()) {
        codes = RetryPolicy.defaults().retryableStatusCodes();
      }
      return new RetryPolicy(maxRetries, codes);
    } catch (Exception ex) {
      return RetryPolicy.defaults();
    }
  }

  private int parseInt(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String stringValue) {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException ex) {
        return fallback;
      }
    }
    return fallback;
  }

  private List<Integer> parseIntList(Object value) {
    if (value instanceof List<?> list) {
      List<Integer> codes = new ArrayList<>();
      for (Object item : list) {
        int parsed = parseInt(item, -1);
        if (parsed > 0) {
          codes.add(parsed);
        }
      }
      return codes;
    }
    return List.of();
  }

  private String readPromptHash(LlmRequest request) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String combined = safe(request.systemPrompt()) + "::" + safe(request.userPrompt());
      byte[] hash = digest.digest(combined.getBytes());
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception ex) {
      return "unknown";
    }
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private void recordAudit(User actor, LlmProvider provider, LlmRequest request, long durationMs, boolean success, Exception error) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("provider", provider.getName());
    payload.put("model", provider.getModel());
    payload.put("success", success);
    payload.put("durationMs", durationMs);
    payload.put("promptHash", readPromptHash(request));
    if (error != null) {
      payload.put("error", error.getClass().getSimpleName());
    }
    auditService.record(actor, "LLM_CALL", "LLM_PROVIDER", payload, provider.getName());
  }

  private String writeJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new LlmCallException("Failed to serialize LLM request", null, false, ex);
    }
  }

  private <T> T readJson(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      throw new LlmCallException("Failed to parse LLM request template", null, false, ex);
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String truncateBody(String body, int maxLength) {
    if (body == null) {
      return null;
    }
    String trimmed = body.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength) + "...(truncated)";
  }

  private void logRequest(LlmProvider provider, URI uri, String body) {
    if (!logger.isInfoEnabled()) {
      return;
    }
    String safeUri = sanitizeUri(provider, uri);
    logger.info("LLM request provider={} model={} uri={} body={}",
        provider != null ? provider.getName() : "unknown",
        provider != null ? provider.getModel() : "unknown",
        safeUri,
        truncateBody(body, 8000));
  }

  private void logResponse(LlmProvider provider, String response) {
    if (!logger.isInfoEnabled()) {
      return;
    }
    logger.info("LLM response provider={} model={} body={}",
        provider != null ? provider.getName() : "unknown",
        provider != null ? provider.getModel() : "unknown",
        truncateBody(response, 8000));
  }

  private String sanitizeUri(LlmProvider provider, URI uri) {
    if (uri == null) {
      return "";
    }
    if (provider == null || provider.getAuthType() != LlmAuthType.QUERY_PARAM) {
      return uri.toString();
    }
    String paramName = provider.getAuthQueryParamName();
    if (paramName == null || paramName.isBlank()) {
      paramName = "key";
    }
    return UriComponentsBuilder.fromUri(uri)
        .replaceQueryParam(paramName)
        .build(true)
        .toUriString();
  }

  private boolean isGeminiProvider(LlmProvider provider) {
    if (provider == null) {
      return false;
    }
    String name = provider.getName();
    if (name != null && "gemini".equalsIgnoreCase(name.trim())) {
      return true;
    }
    String baseUrl = provider.getBaseUrl();
    return baseUrl != null && baseUrl.contains("generativelanguage.googleapis.com");
  }

  private boolean isGeminiModelNotFound(String errorBody) {
    if (errorBody == null || errorBody.isBlank()) {
      return false;
    }
    String normalized = errorBody.toLowerCase(Locale.ROOT);
    return normalized.contains("models/") && normalized.contains("not found");
  }

  private Optional<GeminiModelResolution> resolveGeminiModel(LlmProvider provider) {
    String apiKey = encryptionService.decrypt(provider.getApiKeyEncrypted());
    if (apiKey == null || apiKey.isBlank()) {
      return Optional.empty();
    }
    for (String version : List.of("v1", "v1beta")) {
      Optional<GeminiModelResolution> resolved = listGeminiModels(version, apiKey, provider);
      if (resolved.isPresent()) {
        return resolved;
      }
    }
    return Optional.empty();
  }

  private Optional<GeminiModelResolution> listGeminiModels(String version, String apiKey, LlmProvider provider) {
    URI uri = UriComponentsBuilder.fromUriString("https://generativelanguage.googleapis.com")
        .pathSegment(version, "models")
        .queryParam("key", apiKey)
        .build(true)
        .toUri();
    try {
      RestClient client = buildRestClient(10000);
      String response = client.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
      if (response == null || response.isBlank()) {
        return Optional.empty();
      }
      JsonNode root = objectMapper.readTree(response);
      JsonNode models = root.path("models");
      if (!models.isArray()) {
        return Optional.empty();
      }
      List<String> candidates = new ArrayList<>();
      for (JsonNode modelNode : models) {
        String name = modelNode.path("name").asText(null);
        if (name == null || name.isBlank()) {
          continue;
        }
        JsonNode methods = modelNode.path("supportedGenerationMethods");
        boolean supportsGenerate = false;
        if (methods.isArray()) {
          for (JsonNode method : methods) {
            if ("generateContent".equalsIgnoreCase(method.asText())) {
              supportsGenerate = true;
              break;
            }
          }
        }
        if (!supportsGenerate) {
          continue;
        }
        candidates.add(stripModelName(name));
      }
      Optional<String> chosen = pickGeminiModel(provider, candidates);
      return chosen.map(model -> new GeminiModelResolution(version, model));
    } catch (RestClientResponseException ex) {
      logger.warn("Gemini model list failed status={} message={}", ex.getRawStatusCode(), ex.getMessage());
      return Optional.empty();
    } catch (Exception ex) {
      logger.warn("Gemini model list failed reason={}", ex.getMessage());
      return Optional.empty();
    }
  }

  private Optional<String> pickGeminiModel(LlmProvider provider, List<String> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return Optional.empty();
    }
    String current = provider != null ? safe(provider.getModel()).trim() : "";
    if (!current.isBlank()) {
      for (String candidate : candidates) {
        if (current.equalsIgnoreCase(candidate)) {
          return Optional.of(candidate);
        }
      }
    }
    candidates.sort(Comparator.comparingInt(this::geminiPreferenceScore));
    return Optional.of(candidates.get(0));
  }

  private int geminiPreferenceScore(String model) {
    if (model == null) {
      return Integer.MAX_VALUE;
    }
    String normalized = model.toLowerCase(Locale.ROOT);
    int score = 100;
    if (normalized.contains("gemini")) {
      score -= 30;
    }
    if (normalized.contains("flash")) {
      score -= 10;
    }
    if (normalized.contains("pro")) {
      score -= 5;
    }
    return score;
  }

  private String stripModelName(String name) {
    if (name == null) {
      return "";
    }
    String trimmed = name.trim();
    if (trimmed.startsWith("models/")) {
      return trimmed.substring("models/".length());
    }
    return trimmed;
  }

  private void applyGeminiResolution(LlmProvider provider, GeminiModelResolution resolution) {
    String baseUrl = buildGeminiBaseUrl(resolution.version());
    provider.setModel(resolution.model());
    if (baseUrl != null && !baseUrl.isBlank()) {
      provider.setBaseUrl(baseUrl);
    }
    provider.setUpdatedAt(Instant.now());
    llmProviderRepository.save(provider);
    logger.info("Gemini model resolved to {} ({}).", resolution.model(), resolution.version());
  }

  private String buildGeminiBaseUrl(String version) {
    String normalized = version == null || version.isBlank() ? "v1" : version;
    return "https://generativelanguage.googleapis.com/" + normalized + "/models/{{MODEL}}:generateContent";
  }

  private record GeminiModelResolution(String version, String model) {
  }

  private static class CircuitState {
    private int failures = 0;
    private Instant openUntil;
  }

  private record RetryPolicy(int maxRetries, List<Integer> retryableStatusCodes) {
    private static RetryPolicy defaults() {
      return new RetryPolicy(0, List.of(408, 429, 500, 502, 503, 504));
    }
  }
}
