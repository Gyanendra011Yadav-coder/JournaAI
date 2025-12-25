package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "news_cache")
public class NewsCache {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cache_key", nullable = false, unique = true)
  private String cacheKey;

  @Column(name = "payload_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String payloadJsonb;

  @Column(name = "fetched_at_utc", nullable = false)
  private Instant fetchedAtUtc;

  @Column(name = "expires_at_utc", nullable = false)
  private Instant expiresAtUtc;

  @Column(name = "last_success_at_utc")
  private Instant lastSuccessAtUtc;

  @Column(name = "last_error_code")
  private String lastErrorCode;

  @Column(name = "last_error_message")
  private String lastErrorMessage;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public void setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
  }

  public String getPayloadJsonb() {
    return payloadJsonb;
  }

  public void setPayloadJsonb(String payloadJsonb) {
    this.payloadJsonb = payloadJsonb;
  }

  public Instant getFetchedAtUtc() {
    return fetchedAtUtc;
  }

  public void setFetchedAtUtc(Instant fetchedAtUtc) {
    this.fetchedAtUtc = fetchedAtUtc;
  }

  public Instant getExpiresAtUtc() {
    return expiresAtUtc;
  }

  public void setExpiresAtUtc(Instant expiresAtUtc) {
    this.expiresAtUtc = expiresAtUtc;
  }

  public Instant getLastSuccessAtUtc() {
    return lastSuccessAtUtc;
  }

  public void setLastSuccessAtUtc(Instant lastSuccessAtUtc) {
    this.lastSuccessAtUtc = lastSuccessAtUtc;
  }

  public String getLastErrorCode() {
    return lastErrorCode;
  }

  public void setLastErrorCode(String lastErrorCode) {
    this.lastErrorCode = lastErrorCode;
  }

  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  public void setLastErrorMessage(String lastErrorMessage) {
    this.lastErrorMessage = lastErrorMessage;
  }
}
