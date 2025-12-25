package ai.journa.prcontrol.dto;

public class JournalistArticleSummary {
  private Long articleId;
  private String title;
  private String url;
  private String publishedAtUtc;

  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPublishedAtUtc() {
    return publishedAtUtc;
  }

  public void setPublishedAtUtc(String publishedAtUtc) {
    this.publishedAtUtc = publishedAtUtc;
  }
}
