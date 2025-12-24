package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.news")
public class NewsProviderProperties {
  private int ttlMinutes = 15;
  private int failureThreshold = 3;
  private int circuitMinutes = 30;
  private int searchesPerMinute = 60;
  private int refreshIntervalMinutes = 30;
  private Gnews gnews = new Gnews();

  public int getTtlMinutes() {
    return ttlMinutes;
  }

  public void setTtlMinutes(int ttlMinutes) {
    this.ttlMinutes = ttlMinutes;
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public void setFailureThreshold(int failureThreshold) {
    this.failureThreshold = failureThreshold;
  }

  public int getCircuitMinutes() {
    return circuitMinutes;
  }

  public void setCircuitMinutes(int circuitMinutes) {
    this.circuitMinutes = circuitMinutes;
  }

  public int getSearchesPerMinute() {
    return searchesPerMinute;
  }

  public void setSearchesPerMinute(int searchesPerMinute) {
    this.searchesPerMinute = searchesPerMinute;
  }

  public int getRefreshIntervalMinutes() {
    return refreshIntervalMinutes;
  }

  public void setRefreshIntervalMinutes(int refreshIntervalMinutes) {
    this.refreshIntervalMinutes = refreshIntervalMinutes;
  }

  public Gnews getGnews() {
    return gnews;
  }

  public void setGnews(Gnews gnews) {
    this.gnews = gnews;
  }

  public static class Gnews {
    private String apiKeyBase64;
    private String baseUrl = "https://gnews.io/api/v4";
    private int requestTimeoutSeconds = 10;
    private int maxBackoffSeconds = 30;

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

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public int getRequestTimeoutSeconds() {
      return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
      this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getMaxBackoffSeconds() {
      return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(int maxBackoffSeconds) {
      this.maxBackoffSeconds = maxBackoffSeconds;
    }
  }
}
