package ai.journa.prcontrol.dto;

public class EnrichmentTaskResponse {
  private Long id;
  private String taskType;
  private Long articleId;
  private String articleTitle;
  private String articleUrl;
  private Long journalistId;
  private String journalistName;
  private String status;
  private int priority;
  private int attempts;
  private String nextRunAt;
  private String notes;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public String getArticleTitle() {
    return articleTitle;
  }

  public void setArticleTitle(String articleTitle) {
    this.articleTitle = articleTitle;
  }

  public String getArticleUrl() {
    return articleUrl;
  }

  public void setArticleUrl(String articleUrl) {
    this.articleUrl = articleUrl;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public String getNextRunAt() {
    return nextRunAt;
  }

  public void setNextRunAt(String nextRunAt) {
    this.nextRunAt = nextRunAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
