package ai.journa.prcontrol.dto;

public class ImportJobRowResponse {
  private Long id;
  private int rowNumber;
  private String status;
  private String message;
  private Long journalistId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getRowNumber() {
    return rowNumber;
  }

  public void setRowNumber(int rowNumber) {
    this.rowNumber = rowNumber;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Long getJournalistId() {
    return journalistId;
  }

  public void setJournalistId(Long journalistId) {
    this.journalistId = journalistId;
  }
}
