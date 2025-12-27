package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "admin_approval_queue")
public class AdminApprovalQueueItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id")
  private String entityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_action", nullable = false)
  private AdminApprovalAction changeAction;

  @Column(name = "proposed_changes_jsonb", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String proposedChangesJsonb;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdminApprovalStatus status = AdminApprovalStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by")
  private User requestedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

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

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public AdminApprovalAction getChangeAction() {
    return changeAction;
  }

  public void setChangeAction(AdminApprovalAction changeAction) {
    this.changeAction = changeAction;
  }

  public String getProposedChangesJsonb() {
    return proposedChangesJsonb;
  }

  public void setProposedChangesJsonb(String proposedChangesJsonb) {
    this.proposedChangesJsonb = proposedChangesJsonb;
  }

  public AdminApprovalStatus getStatus() {
    return status;
  }

  public void setStatus(AdminApprovalStatus status) {
    this.status = status;
  }

  public User getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(User requestedBy) {
    this.requestedBy = requestedBy;
  }

  public User getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(User reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
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
