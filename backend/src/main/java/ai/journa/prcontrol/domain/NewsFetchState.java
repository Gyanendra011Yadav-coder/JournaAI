package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "news_fetch_state")
public class NewsFetchState {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id", nullable = false)
  private Beat beat;

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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Beat getBeat() {
    return beat;
  }

  public void setBeat(Beat beat) {
    this.beat = beat;
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
}
