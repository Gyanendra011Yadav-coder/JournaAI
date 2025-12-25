package ai.journa.prcontrol.service.integration;

import java.util.List;

public class FetchResult {
  private int totalArticles;
  private List<ProviderArticle> articles;
  private String rawPayload;

  public FetchResult() {
  }

  public FetchResult(int totalArticles, List<ProviderArticle> articles) {
    this.totalArticles = totalArticles;
    this.articles = articles;
  }

  public int getTotalArticles() {
    return totalArticles;
  }

  public void setTotalArticles(int totalArticles) {
    this.totalArticles = totalArticles;
  }

  public List<ProviderArticle> getArticles() {
    return articles;
  }

  public void setArticles(List<ProviderArticle> articles) {
    this.articles = articles;
  }

  public String getRawPayload() {
    return rawPayload;
  }

  public void setRawPayload(String rawPayload) {
    this.rawPayload = rawPayload;
  }
}
