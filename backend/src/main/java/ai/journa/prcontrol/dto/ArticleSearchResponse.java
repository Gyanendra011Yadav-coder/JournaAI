package ai.journa.prcontrol.dto;

import java.time.Instant;
import java.util.List;

public class ArticleSearchResponse {
    private List<ArticleResponse> items;
    private long total;
    private int page;
    private int size;
    private Instant lastRefreshedAt;

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

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }
}
