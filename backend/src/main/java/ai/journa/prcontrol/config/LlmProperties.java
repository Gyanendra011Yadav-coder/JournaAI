package ai.journa.prcontrol.config;

import ai.journa.prcontrol.domain.LlmAuthType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
  private boolean enabled = true;
  private Seed seed = new Seed();
  private AuthorExtraction authorExtraction = new AuthorExtraction();
  private CircuitBreaker circuitBreaker = new CircuitBreaker();
  private List<String> providerOrder = new ArrayList<>();
  private List<Provider> providers = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Seed getSeed() {
    return seed;
  }

  public void setSeed(Seed seed) {
    this.seed = seed;
  }

  public AuthorExtraction getAuthorExtraction() {
    return authorExtraction;
  }

  public void setAuthorExtraction(AuthorExtraction authorExtraction) {
    this.authorExtraction = authorExtraction;
  }

  public CircuitBreaker getCircuitBreaker() {
    return circuitBreaker;
  }

  public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
    this.circuitBreaker = circuitBreaker;
  }

  public List<String> getProviderOrder() {
    return providerOrder;
  }

  public void setProviderOrder(List<String> providerOrder) {
    this.providerOrder = providerOrder;
  }

  public List<Provider> getProviders() {
    return providers;
  }

  public void setProviders(List<Provider> providers) {
    this.providers = providers;
  }

  public static class Seed {
    private boolean enabled = true;
    private boolean updateExisting = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isUpdateExisting() {
      return updateExisting;
    }

    public void setUpdateExisting(boolean updateExisting) {
      this.updateExisting = updateExisting;
    }
  }

  public static class AuthorExtraction {
    private boolean enabled = true;
    private int cacheDays = 7;
    private int maxAttempts = 2;
    private int cooldownMinutes = 60;
    private int maxTokens = 512;
    private double temperature = 0.2;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getCacheDays() {
      return cacheDays;
    }

    public void setCacheDays(int cacheDays) {
      this.cacheDays = cacheDays;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public int getCooldownMinutes() {
      return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
      this.cooldownMinutes = cooldownMinutes;
    }

    public int getMaxTokens() {
      return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
    }

    public double getTemperature() {
      return temperature;
    }

    public void setTemperature(double temperature) {
      this.temperature = temperature;
    }
  }

  public static class CircuitBreaker {
    private int failureThreshold = 3;
    private int openMinutes = 20;

    public int getFailureThreshold() {
      return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
      this.failureThreshold = failureThreshold;
    }

    public int getOpenMinutes() {
      return openMinutes;
    }

    public void setOpenMinutes(int openMinutes) {
      this.openMinutes = openMinutes;
    }
  }

  public static class Provider {
    private String name;
    private boolean enabled = true;
    private String baseUrl;
    private LlmAuthType authType = LlmAuthType.API_KEY_HEADER;
    private String authHeaderName;
    private String authQueryParamName;
    private String apiKeyBase64;
    private String model;
    private Map<String, Object> requestTemplate = new HashMap<>();
    private String responseJsonpath;
    private Integer timeoutMs;
    private Map<String, Object> retryPolicy = new HashMap<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public LlmAuthType getAuthType() {
      return authType;
    }

    public void setAuthType(LlmAuthType authType) {
      this.authType = authType;
    }

    public String getAuthHeaderName() {
      return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
      this.authHeaderName = authHeaderName;
    }

    public String getAuthQueryParamName() {
      return authQueryParamName;
    }

    public void setAuthQueryParamName(String authQueryParamName) {
      this.authQueryParamName = authQueryParamName;
    }

    public String getApiKeyBase64() {
      return apiKeyBase64;
    }

    public void setApiKeyBase64(String apiKeyBase64) {
      this.apiKeyBase64 = apiKeyBase64;
    }

    public String getDecodedApiKey() {
      if (apiKeyBase64 == null || apiKeyBase64.isBlank()) {
        return null;
      }
      try {
        byte[] decoded = Base64.getDecoder().decode(apiKeyBase64);
        return new String(decoded, StandardCharsets.UTF_8).trim();
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public Map<String, Object> getRequestTemplate() {
      return requestTemplate;
    }

    public void setRequestTemplate(Map<String, Object> requestTemplate) {
      this.requestTemplate = requestTemplate;
    }

    public String getResponseJsonpath() {
      return responseJsonpath;
    }

    public void setResponseJsonpath(String responseJsonpath) {
      this.responseJsonpath = responseJsonpath;
    }

    public Integer getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
      this.timeoutMs = timeoutMs;
    }

    public Map<String, Object> getRetryPolicy() {
      return retryPolicy;
    }

    public void setRetryPolicy(Map<String, Object> retryPolicy) {
      this.retryPolicy = retryPolicy;
    }
  }
}
