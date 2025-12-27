package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "article_author_extractions")
public class ArticleAuthorExtraction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @Column(name = "author_raw")
  private String authorRaw;

  @Column(name = "candidates_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String candidatesJsonb;

  @Enumerated(EnumType.STRING)
  @Column(name = "method")
  private AuthorExtractionMethod method;

  @Column(name = "authors_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String authorsJsonb;

  @Enumerated(EnumType.STRING)
  @Column(name = "classification")
  private AuthorClassification classification;

  private Integer confidence;

  @Column(name = "evidence_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String evidenceJsonb;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private AuthorExtractionStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts = 0;

  @Column(name = "content_hash")
  private String contentHash;

  @Column(name = "is_non_person_author", nullable = false)
  private boolean nonPersonAuthor;

  @Enumerated(EnumType.STRING)
  @Column(name = "fetch_status", nullable = false)
  private FetchStatus fetchStatus;

  @Column(name = "extracted_at")
  private Instant extractedAt;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "error_message")
  private String errorMessage;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public String getAuthorRaw() {
    return authorRaw;
  }

  public void setAuthorRaw(String authorRaw) {
    this.authorRaw = authorRaw;
  }

  public String getCandidatesJsonb() {
    return candidatesJsonb;
  }

  public void setCandidatesJsonb(String candidatesJsonb) {
    this.candidatesJsonb = candidatesJsonb;
  }

  public AuthorExtractionMethod getMethod() {
    return method;
  }

  public void setMethod(AuthorExtractionMethod method) {
    this.method = method;
  }

  public String getAuthorsJsonb() {
    return authorsJsonb;
  }

  public void setAuthorsJsonb(String authorsJsonb) {
    this.authorsJsonb = authorsJsonb;
  }

  public AuthorClassification getClassification() {
    return classification;
  }

  public void setClassification(AuthorClassification classification) {
    this.classification = classification;
  }

  public Integer getConfidence() {
    return confidence;
  }

  public void setConfidence(Integer confidence) {
    this.confidence = confidence;
  }

  public String getEvidenceJsonb() {
    return evidenceJsonb;
  }

  public void setEvidenceJsonb(String evidenceJsonb) {
    this.evidenceJsonb = evidenceJsonb;
  }

  public AuthorExtractionStatus getStatus() {
    return status;
  }

  public void setStatus(AuthorExtractionStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(String contentHash) {
    this.contentHash = contentHash;
  }

  public boolean isNonPersonAuthor() {
    return nonPersonAuthor;
  }

  public void setNonPersonAuthor(boolean nonPersonAuthor) {
    this.nonPersonAuthor = nonPersonAuthor;
  }

  public FetchStatus getFetchStatus() {
    return fetchStatus;
  }

  public void setFetchStatus(FetchStatus fetchStatus) {
    this.fetchStatus = fetchStatus;
  }

  public Instant getExtractedAt() {
    return extractedAt;
  }

  public void setExtractedAt(Instant extractedAt) {
    this.extractedAt = extractedAt;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
