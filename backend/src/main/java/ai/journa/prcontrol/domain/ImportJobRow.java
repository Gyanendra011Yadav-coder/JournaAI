package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "import_job_rows")
public class ImportJobRow {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private ImportJob job;

  @Column(name = "row_number", nullable = false)
  private int rowNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImportRowStatus status = ImportRowStatus.SKIPPED;

  private String message;

  @Column(name = "payload_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String payloadJsonb;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalist_id")
  private Journalist journalist;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ImportJob getJob() {
    return job;
  }

  public void setJob(ImportJob job) {
    this.job = job;
  }

  public int getRowNumber() {
    return rowNumber;
  }

  public void setRowNumber(int rowNumber) {
    this.rowNumber = rowNumber;
  }

  public ImportRowStatus getStatus() {
    return status;
  }

  public void setStatus(ImportRowStatus status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPayloadJsonb() {
    return payloadJsonb;
  }

  public void setPayloadJsonb(String payloadJsonb) {
    this.payloadJsonb = payloadJsonb;
  }

  public Journalist getJournalist() {
    return journalist;
  }

  public void setJournalist(Journalist journalist) {
    this.journalist = journalist;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
