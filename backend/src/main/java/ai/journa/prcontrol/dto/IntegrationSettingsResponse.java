package ai.journa.prcontrol.dto;

public class IntegrationSettingsResponse {
  private String providerType;
  private boolean enabled;
  private boolean configured;
  private String defaultLang;
  private String defaultCountry;
  private int refreshIntervalMinutes;
  private int ttlMinutes;
  private int maxPerRequest;
  private String updatedAt;
  private String updatedBy;

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
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

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }
}
