package ai.journa.prcontrol.dto;

public class AdminArticleUpdateRequest {
  private String title;
  private String description;
  private String content;
  private String imageUrl;
  private String sourceName;
  private String sourceUrl;
  private String sourceCountry;
  private String category;
  private String publishedAtUtc;
  private String authorRaw;
  private Long journalistId;
  private Integer journalistMatchConfidence;

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

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getPublishedAtUtc() {
    return publishedAtUtc;
  }

  public void setPublishedAtUtc(String publishedAtUtc) {
    this.publishedAtUtc = publishedAtUtc;
  }

  public String getAuthorRaw() {
    return authorRaw;
  }

  public void setAuthorRaw(String authorRaw) {
    this.authorRaw = authorRaw;
  }

  public Long getJournalistId() {
    return journalistId;
  }

  public void setJournalistId(Long journalistId) {
    this.journalistId = journalistId;
  }

  public Integer getJournalistMatchConfidence() {
    return journalistMatchConfidence;
  }

  public void setJournalistMatchConfidence(Integer journalistMatchConfidence) {
    this.journalistMatchConfidence = journalistMatchConfidence;
  }
}
