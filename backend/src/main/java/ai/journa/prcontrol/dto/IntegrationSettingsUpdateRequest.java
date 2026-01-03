package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class IntegrationSettingsUpdateRequest {
  @NotNull
  private Boolean enabled;

  @NotBlank
  private String defaultLang;

  @NotBlank
  private String defaultCountry;

  @Min(1)
  private int refreshIntervalMinutes;

  @Min(1)
  private int ttlMinutes;

  @Min(1)
  private int maxPerRequest;

  private String searchEngineId;

  private String allowedDomains;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
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

  public int getRefreshIntervalMinutes() {
    return refreshIntervalMinutes;
  }

  public void setRefreshIntervalMinutes(int refreshIntervalMinutes) {
    this.refreshIntervalMinutes = refreshIntervalMinutes;
  }

  public int getTtlMinutes() {
    return ttlMinutes;
  }

  public void setTtlMinutes(int ttlMinutes) {
    this.ttlMinutes = ttlMinutes;
  }

  public int getMaxPerRequest() {
    return maxPerRequest;
  }

  public void setMaxPerRequest(int maxPerRequest) {
    this.maxPerRequest = maxPerRequest;
  }

  public String getSearchEngineId() {
    return searchEngineId;
  }

  public void setSearchEngineId(String searchEngineId) {
    this.searchEngineId = searchEngineId;
  }

  public String getAllowedDomains() {
    return allowedDomains;
  }

  public void setAllowedDomains(String allowedDomains) {
    this.allowedDomains = allowedDomains;
  }
}
