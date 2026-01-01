package ai.journa.prcontrol.dto;

public class ArticleResponse {
  private Long id;
  private String providerType;
  private String providerArticleId;
  private Long beatId;
  private String beatName;
  private String category;
  private String lensSource;
  private boolean clientMatch;
  private String title;
  private String description;
  private String content;
  private String url;
  private String imageUrl;
  private String publishedAtUtc;
  private String lang;
  private String sourceId;
  private String sourceName;
  private String sourceUrl;
  private String sourceCountry;
  private String authorRaw;
  private Long journalistId;
  private String journalistName;
  private String authorTaskStatus;
  private String fetchedAtUtc;
  private String status;
  private String publishedBy;
  private String internalPublishedAtUtc;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getProviderArticleId() {
    return providerArticleId;
  }

  public void setProviderArticleId(String providerArticleId) {
    this.providerArticleId = providerArticleId;
  }

  public Long getBeatId() {
    return beatId;
  }

  public void setBeatId(Long beatId) {
    this.beatId = beatId;
  }

  public String getBeatName() {
    return beatName;
  }

  public void setBeatName(String beatName) {
    this.beatName = beatName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getLensSource() {
    return lensSource;
  }

  public void setLensSource(String lensSource) {
    this.lensSource = lensSource;
  }

  public boolean isClientMatch() {
    return clientMatch;
  }

  public void setClientMatch(boolean clientMatch) {
    this.clientMatch = clientMatch;
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

  public String getPublishedAtUtc() {
    return publishedAtUtc;
  }

  public void setPublishedAtUtc(String publishedAtUtc) {
    this.publishedAtUtc = publishedAtUtc;
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

  public String getJournalistName() {
    return journalistName;
  }

  public void setJournalistName(String journalistName) {
    this.journalistName = journalistName;
  }

  public String getAuthorTaskStatus() {
    return authorTaskStatus;
  }

  public void setAuthorTaskStatus(String authorTaskStatus) {
    this.authorTaskStatus = authorTaskStatus;
  }

  public String getFetchedAtUtc() {
    return fetchedAtUtc;
  }

  public void setFetchedAtUtc(String fetchedAtUtc) {
    this.fetchedAtUtc = fetchedAtUtc;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPublishedBy() {
    return publishedBy;
  }

  public void setPublishedBy(String publishedBy) {
    this.publishedBy = publishedBy;
  }

  public String getInternalPublishedAtUtc() {
    return internalPublishedAtUtc;
  }

  public void setInternalPublishedAtUtc(String internalPublishedAtUtc) {
    this.internalPublishedAtUtc = internalPublishedAtUtc;
  }
}
