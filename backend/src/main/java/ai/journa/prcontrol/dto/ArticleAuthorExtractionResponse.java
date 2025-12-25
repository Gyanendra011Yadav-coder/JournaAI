package ai.journa.prcontrol.dto;

import java.util.List;

public class ArticleAuthorExtractionResponse {
  private Long articleId;
  private String authorRaw;
  private List<AuthorCandidateDto> candidates;
  private boolean nonPersonAuthor;
  private String fetchStatus;
  private String extractedAt;
  private String errorCode;
  private String errorMessage;

  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public String getAuthorRaw() {
    return authorRaw;
  }

  public void setAuthorRaw(String authorRaw) {
    this.authorRaw = authorRaw;
  }

  public List<AuthorCandidateDto> getCandidates() {
    return candidates;
  }

  public void setCandidates(List<AuthorCandidateDto> candidates) {
    this.candidates = candidates;
  }

  public boolean isNonPersonAuthor() {
    return nonPersonAuthor;
  }

  public void setNonPersonAuthor(boolean nonPersonAuthor) {
    this.nonPersonAuthor = nonPersonAuthor;
  }

  public String getFetchStatus() {
    return fetchStatus;
  }

  public void setFetchStatus(String fetchStatus) {
    this.fetchStatus = fetchStatus;
  }

  public String getExtractedAt() {
    return extractedAt;
  }

  public void setExtractedAt(String extractedAt) {
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
