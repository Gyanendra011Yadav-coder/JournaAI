package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.search")
public class SearchProviderProperties {
  private Google google = new Google();

  public Google getGoogle() {
    return google;
  }

  public void setGoogle(Google google) {
    this.google = google;
  }

  public static class Google {
    private String apiKeyBase64;
    private String searchEngineId;
    private String baseUrl = "https://www.googleapis.com/customsearch/v1";
    private int requestTimeoutSeconds = 10;
    private int maxResults = 5;
    private String defaultLang = "en";
    private String defaultCountry = "us";
    private String allowedDomains = "*";

    public String getApiKeyBase64() {
      return apiKeyBase64;
    }

    public void setApiKeyBase64(String apiKeyBase64) {
      this.apiKeyBase64 = apiKeyBase64;
    }

    public String getSearchEngineId() {
      return searchEngineId;
    }

    public void setSearchEngineId(String searchEngineId) {
      this.searchEngineId = searchEngineId;
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

    public int getMaxResults() {
      return maxResults;
    }

    public void setMaxResults(int maxResults) {
      this.maxResults = maxResults;
    }

    public String getDefaultLang() {
      return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
      this.defaultLang = defaultLang;
    }

    public String getDefaultCountry() {
      return defaultCountry;
    }

    public void setDefaultCountry(String defaultCountry) {
      this.defaultCountry = defaultCountry;
    }

    public String getAllowedDomains() {
      return allowedDomains;
    }

    public void setAllowedDomains(String allowedDomains) {
      this.allowedDomains = allowedDomains;
    }
  }
}
