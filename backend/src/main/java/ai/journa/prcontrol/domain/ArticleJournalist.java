package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "article_journalists")
public class ArticleJournalist {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalist_id", nullable = false)
  private Journalist journalist;

  @Column(name = "match_confidence", nullable = false)
  private int matchConfidence;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_method", nullable = false)
  private MatchMethod matchMethod;

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

  public Journalist getJournalist() {
    return journalist;
  }

  public void setJournalist(Journalist journalist) {
    this.journalist = journalist;
  }

  public int getMatchConfidence() {
    return matchConfidence;
  }

  public void setMatchConfidence(int matchConfidence) {
    this.matchConfidence = matchConfidence;
  }

  public MatchMethod getMatchMethod() {
    return matchMethod;
  }

  public void setMatchMethod(MatchMethod matchMethod) {
    this.matchMethod = matchMethod;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
