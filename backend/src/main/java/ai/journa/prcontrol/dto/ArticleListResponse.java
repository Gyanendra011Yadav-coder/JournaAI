package ai.journa.prcontrol.dto;

import java.util.List;

public class ArticleListResponse {
  private List<ArticleResponse> items;
  private long total;
  private int page;
  private int size;
  private String lastRefreshedAt;
  private boolean staleCache;

  public List<ArticleResponse> getItems() {
    return items;
  }

  public void setItems(List<ArticleResponse> items) {
    this.items = items;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getLastRefreshedAt() {
    return lastRefreshedAt;
  }

  public void setLastRefreshedAt(String lastRefreshedAt) {
    this.lastRefreshedAt = lastRefreshedAt;
  }

  public boolean isStaleCache() {
    return staleCache;
  }

  public void setStaleCache(boolean staleCache) {
    this.staleCache = staleCache;
  }
}
