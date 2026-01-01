package ai.journa.prcontrol.dto;

public class LlmProviderResponse {
  private Long id;
  private String name;
  private boolean enabled;
  private boolean configured;
  private String baseUrl;
  private String authType;
  private String authHeaderName;
  private String authQueryParamName;
  private String model;
  private String requestTemplateJsonb;
  private String responseJsonpath;
  private int timeoutMs;
  private String retryPolicyJsonb;
  private String updatedAt;
  private String updatedBy;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getAuthType() {
    return authType;
  }

  public void setAuthType(String authType) {
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

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getRequestTemplateJsonb() {
    return requestTemplateJsonb;
  }

  public void setRequestTemplateJsonb(String requestTemplateJsonb) {
    this.requestTemplateJsonb = requestTemplateJsonb;
  }

  public String getResponseJsonpath() {
    return responseJsonpath;
  }

  public void setResponseJsonpath(String responseJsonpath) {
    this.responseJsonpath = responseJsonpath;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public String getRetryPolicyJsonb() {
    return retryPolicyJsonb;
  }

  public void setRetryPolicyJsonb(String retryPolicyJsonb) {
    this.retryPolicyJsonb = retryPolicyJsonb;
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
