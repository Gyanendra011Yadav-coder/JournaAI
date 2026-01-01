package ai.journa.prcontrol.dto;

import ai.journa.prcontrol.domain.LlmAuthType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LlmProviderUpdateRequest {
  @NotNull
  private Boolean enabled;

  @NotBlank
  private String baseUrl;

  @NotNull
  private LlmAuthType authType;

  private String authHeaderName;
  private String authQueryParamName;
  private String model;

  @NotBlank
  private String requestTemplateJsonb;

  @NotBlank
  private String responseJsonpath;

  @Min(1000)
  private Integer timeoutMs;

  private String retryPolicyJsonb;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
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

  public Integer getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public String getRetryPolicyJsonb() {
    return retryPolicyJsonb;
  }

  public void setRetryPolicyJsonb(String retryPolicyJsonb) {
    this.retryPolicyJsonb = retryPolicyJsonb;
  }
}
