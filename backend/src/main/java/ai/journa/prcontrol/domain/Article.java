package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "articles")
public class Article {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider_type", nullable = false)
  private ProviderType providerType;

  @Column(name = "provider_article_id")
  private String providerArticleId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id", nullable = false)
  private Beat beat;

  @Column(nullable = false)
  private String title;

  private String description;

  private String content;

  @Column(nullable = false)
  private String url;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "published_at_utc")
  private Instant providerPublishedAtUtc;

  private String lang;

  @Column(name = "source_id")
  private String sourceId;

  @Column(name = "source_name")
  private String sourceName;

  @Column(name = "source_url")
  private String sourceUrl;

  @Column(name = "source_country")
  private String sourceCountry;

  @Column(name = "raw_payload_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String rawPayloadJsonb;

  @Column(name = "fetched_at_utc", nullable = false)
  private Instant fetchedAtUtc = Instant.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ArticleStatus status = ArticleStatus.INGESTED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "published_by")
  private User publishedBy;

  @Column(name = "internal_published_at_utc")
  private Instant internalPublishedAtUtc;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProviderType getProviderType() {
    return providerType;
  }

  public void setProviderType(ProviderType providerType) {
    this.providerType = providerType;
  }

  public String getProviderArticleId() {
    return providerArticleId;
  }

  public void setProviderArticleId(String providerArticleId) {
    this.providerArticleId = providerArticleId;
  }

  public Beat getBeat() {
    return beat;
  }

  public void setBeat(Beat beat) {
    this.beat = beat;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public Instant getProviderPublishedAtUtc() {
    return providerPublishedAtUtc;
  }

  public void setProviderPublishedAtUtc(Instant publishedAtUtc) {
    this.providerPublishedAtUtc = publishedAtUtc;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public String getSourceCountry() {
    return sourceCountry;
  }

  public void setSourceCountry(String sourceCountry) {
    this.sourceCountry = sourceCountry;
  }

  public String getRawPayloadJsonb() {
    return rawPayloadJsonb;
  }

  public void setRawPayloadJsonb(String rawPayloadJsonb) {
    this.rawPayloadJsonb = rawPayloadJsonb;
  }

  public Instant getFetchedAtUtc() {
    return fetchedAtUtc;
  }

  public void setFetchedAtUtc(Instant fetchedAtUtc) {
    this.fetchedAtUtc = fetchedAtUtc;
  }

  public ArticleStatus getStatus() {
    return status;
  }

  public void setStatus(ArticleStatus status) {
    this.status = status;
  }

  public User getPublishedBy() {
    return publishedBy;
  }

  public void setPublishedBy(User publishedBy) {
    this.publishedBy = publishedBy;
  }

  public Instant getInternalPublishedAtUtc() {
    return internalPublishedAtUtc;
  }

  public void setInternalPublishedAtUtc(Instant internalPublishedAtUtc) {
    this.internalPublishedAtUtc = internalPublishedAtUtc;
  }
}
