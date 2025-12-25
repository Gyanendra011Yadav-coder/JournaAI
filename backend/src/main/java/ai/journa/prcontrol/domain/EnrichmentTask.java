package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "enrichment_tasks")
public class EnrichmentTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false)
  private EnrichmentTaskType taskType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalist_id")
  private Journalist journalist;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrichmentTaskStatus status = EnrichmentTaskStatus.PENDING;

  @Column(nullable = false)
  private int priority = 0;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(name = "next_run_at")
  private Instant nextRunAt;

  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public EnrichmentTaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(EnrichmentTaskType taskType) {
    this.taskType = taskType;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public Journalist getJournalist() {
    return journalist;
  }

  public void setJournalist(Journalist journalist) {
    this.journalist = journalist;
  }

  public EnrichmentTaskStatus getStatus() {
    return status;
  }

  public void setStatus(EnrichmentTaskStatus status) {
    this.status = status;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getNextRunAt() {
    return nextRunAt;
  }

  public void setNextRunAt(Instant nextRunAt) {
    this.nextRunAt = nextRunAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
}
