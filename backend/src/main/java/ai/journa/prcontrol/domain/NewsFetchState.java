package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "news_fetch_state")
public class NewsFetchState {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "key", nullable = false, unique = true)
  private String key;

  @Column(name = "mode", nullable = false)
  private String mode;

  @Column(name = "lens_or_track", nullable = false)
  private String lensOrTrack;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id")
  private Beat beat;

  @Column(name = "category")
  private String category;

  @Column(name = "last_success_at")
  private Instant lastSuccessAt;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "last_error_code")
  private String lastErrorCode;

  @Column(name = "last_error_message")
  private String lastErrorMessage;

  @Column(name = "consecutive_failures", nullable = false)
  private int consecutiveFailures;

  @Column(name = "cooldown_until")
  private Instant cooldownUntil;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getLensOrTrack() {
    return lensOrTrack;
  }

  public void setLensOrTrack(String lensOrTrack) {
    this.lensOrTrack = lensOrTrack;
  }

  public Beat getBeat() {
    return beat;
  }

  public void setBeat(Beat beat) {
    this.beat = beat;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Instant getLastSuccessAt() {
    return lastSuccessAt;
  }

  public void setLastSuccessAt(Instant lastSuccessAt) {
    this.lastSuccessAt = lastSuccessAt;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public void setLastAttemptAt(Instant lastAttemptAt) {
    this.lastAttemptAt = lastAttemptAt;
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

  public int getConsecutiveFailures() {
    return consecutiveFailures;
  }

  public void setConsecutiveFailures(int consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
  }

  public Instant getCooldownUntil() {
    return cooldownUntil;
  }

  public void setCooldownUntil(Instant cooldownUntil) {
    this.cooldownUntil = cooldownUntil;
  }
}
