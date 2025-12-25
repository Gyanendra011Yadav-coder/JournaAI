package ai.journa.prcontrol.dto;

public class SavedArticleResponse {
  private Long articleId;
  private boolean pinned;
  private String savedAt;
  private String note;
  private String[] tags;
  private ArticleResponse article;

  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public boolean isPinned() {
    return pinned;
  }

  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

  public String getSavedAt() {
    return savedAt;
  }

  public void setSavedAt(String savedAt) {
    this.savedAt = savedAt;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public ArticleResponse getArticle() {
    return article;
  }

  public void setArticle(ArticleResponse article) {
    this.article = article;
  }
}
