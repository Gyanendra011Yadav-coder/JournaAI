package ai.journa.prcontrol.dto;

public class RefreshResponse {
  private String status;
  private boolean staleCache;
  private String lastRefreshedAt;
  private String message;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isStaleCache() {
    return staleCache;
  }

  public void setStaleCache(boolean staleCache) {
    this.staleCache = staleCache;
  }

  public String getLastRefreshedAt() {
    return lastRefreshedAt;
  }

  public void setLastRefreshedAt(String lastRefreshedAt) {
    this.lastRefreshedAt = lastRefreshedAt;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
