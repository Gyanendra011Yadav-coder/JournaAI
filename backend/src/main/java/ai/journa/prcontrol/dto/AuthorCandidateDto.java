package ai.journa.prcontrol.dto;

public class AuthorCandidateDto {
  private String name;
  private int confidence;
  private String method;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getConfidence() {
    return confidence;
  }

  public void setConfidence(int confidence) {
    this.confidence = confidence;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }
}
