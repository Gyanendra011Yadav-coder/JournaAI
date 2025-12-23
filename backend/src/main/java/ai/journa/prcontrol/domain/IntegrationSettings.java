package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "integration_settings")
public class IntegrationSettings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider_type", nullable = false)
  private ProviderType providerType;

  @Column(name = "is_enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "api_key_encrypted")
  private String apiKeyEncrypted;

  @Column(name = "default_lang")
  private String defaultLang;

  @Column(name = "default_country")
  private String defaultCountry;

  @Column(name = "refresh_interval_minutes", nullable = false)
  private Integer refreshIntervalMinutes = 30;

  @Column(name = "ttl_minutes", nullable = false)
  private Integer ttlMinutes = 15;

  @Column(name = "max_per_request", nullable = false)
  private Integer maxPerRequest = 50;

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

  public ProviderType getProviderType() {
    return providerType;
  }

  public void setProviderType(ProviderType providerType) {
    this.providerType = providerType;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApiKeyEncrypted() {
    return apiKeyEncrypted;
  }

  public void setApiKeyEncrypted(String apiKeyEncrypted) {
    this.apiKeyEncrypted = apiKeyEncrypted;
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

  public Integer getRefreshIntervalMinutes() {
    return refreshIntervalMinutes;
  }

  public void setRefreshIntervalMinutes(Integer refreshIntervalMinutes) {
    this.refreshIntervalMinutes = refreshIntervalMinutes;
  }

  public Integer getTtlMinutes() {
    return ttlMinutes;
  }

  public void setTtlMinutes(Integer ttlMinutes) {
    this.ttlMinutes = ttlMinutes;
  }

  public Integer getMaxPerRequest() {
    return maxPerRequest;
  }

  public void setMaxPerRequest(Integer maxPerRequest) {
    this.maxPerRequest = maxPerRequest;
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
