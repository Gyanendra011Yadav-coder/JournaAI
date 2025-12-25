package ai.journa.prcontrol.dto;

public class RefreshResponse {
  private String status;
  private boolean staleCache;
  private String lastRefreshedAt;
  private String message;
  private String resolvedCountry;
  private String resolvedLang;

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

  public String getResolvedCountry() {
    return resolvedCountry;
  }

  public void setResolvedCountry(String resolvedCountry) {
    this.resolvedCountry = resolvedCountry;
  }

  public String getResolvedLang() {
    return resolvedLang;
  }

  public void setResolvedLang(String resolvedLang) {
    this.resolvedLang = resolvedLang;
  }
}
