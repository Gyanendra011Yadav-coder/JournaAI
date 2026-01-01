package ai.journa.prcontrol.dto;

public class JournalistEnrichmentReviewResponse {
  private Long id;
  private Long journalistId;
  private String journalistName;
  private String status;
  private String currentJsonb;
  private String proposedJsonb;
  private String diffJsonb;
  private String createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getCurrentJsonb() {
    return currentJsonb;
  }

  public void setCurrentJsonb(String currentJsonb) {
    this.currentJsonb = currentJsonb;
  }

  public String getProposedJsonb() {
    return proposedJsonb;
  }

  public void setProposedJsonb(String proposedJsonb) {
    this.proposedJsonb = proposedJsonb;
  }

  public String getDiffJsonb() {
    return diffJsonb;
  }

  public void setDiffJsonb(String diffJsonb) {
    this.diffJsonb = diffJsonb;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
