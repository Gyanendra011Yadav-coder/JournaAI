package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "article_attributions")
public class ArticleAttribution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @Column(name = "bureau_name", nullable = false)
  private String bureauName;

  @Enumerated(EnumType.STRING)
  @Column(name = "attribution_type", nullable = false)
  private AttributionType attributionType;

  private Integer confidence;

  @Column(name = "evidence_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String evidenceJsonb;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

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

  public String getBureauName() {
    return bureauName;
  }

  public void setBureauName(String bureauName) {
    this.bureauName = bureauName;
  }

  public AttributionType getAttributionType() {
    return attributionType;
  }

  public void setAttributionType(AttributionType attributionType) {
    this.attributionType = attributionType;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
