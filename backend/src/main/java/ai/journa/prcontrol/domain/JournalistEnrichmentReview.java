package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "journalist_enrichment_reviews")
public class JournalistEnrichmentReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalist_id", nullable = false)
  private Journalist journalist;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JournalistEnrichmentReviewStatus status = JournalistEnrichmentReviewStatus.PENDING;

  @Column(name = "current_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String currentJsonb;

  @Column(name = "proposed_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String proposedJsonb;

  @Column(name = "diff_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String diffJsonb;

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

  public Journalist getJournalist() {
    return journalist;
  }

  public void setJournalist(Journalist journalist) {
    this.journalist = journalist;
  }

  public JournalistEnrichmentReviewStatus getStatus() {
    return status;
  }

  public void setStatus(JournalistEnrichmentReviewStatus status) {
    this.status = status;
  }

  public String getCurrentJsonb() {
    return currentJsonb;
  }

  public void setCurrentJsonb(String currentJsonb) {
    this.currentJsonb = currentJsonb;
  }

  public String getProposedJsonb() {
    return proposedJsonb;
  }

  public void setProposedJsonb(String proposedJsonb) {
    this.proposedJsonb = proposedJsonb;
  }

  public String getDiffJsonb() {
    return diffJsonb;
  }

  public void setDiffJsonb(String diffJsonb) {
    this.diffJsonb = diffJsonb;
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
