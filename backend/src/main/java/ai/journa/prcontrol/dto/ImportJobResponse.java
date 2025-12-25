package ai.journa.prcontrol.dto;

import java.util.List;

public class ImportJobResponse {
  private Long id;
  private String status;
  private boolean dryRun;
  private String createdAt;
  private String sourceName;
  private String summaryJsonb;
  private List<ImportJobRowResponse> rows;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getSummaryJsonb() {
    return summaryJsonb;
  }

  public void setSummaryJsonb(String summaryJsonb) {
    this.summaryJsonb = summaryJsonb;
  }

  public List<ImportJobRowResponse> getRows() {
    return rows;
  }

  public void setRows(List<ImportJobRowResponse> rows) {
    this.rows = rows;
  }
}
