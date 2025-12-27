package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "llm_providers")
public class LlmProvider {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "base_url", nullable = false)
  private String baseUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_type", nullable = false)
  private LlmAuthType authType;

  @Column(name = "auth_header_name")
  private String authHeaderName;

  @Column(name = "auth_query_param_name")
  private String authQueryParamName;

  @Column(name = "api_key_encrypted")
  private String apiKeyEncrypted;

  private String model;

  @Column(name = "request_template_jsonb", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String requestTemplateJsonb;

  @Column(name = "response_jsonpath", nullable = false)
  private String responseJsonpath;

  @Column(name = "timeout_ms", nullable = false)
  private int timeoutMs = 10000;

  @Column(name = "retry_policy_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String retryPolicyJsonb;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

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

  public String getApiKeyEncrypted() {
    return apiKeyEncrypted;
  }

  public void setApiKeyEncrypted(String apiKeyEncrypted) {
    this.apiKeyEncrypted = apiKeyEncrypted;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public User getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(User updatedBy) {
    this.updatedBy = updatedBy;
  }
}
