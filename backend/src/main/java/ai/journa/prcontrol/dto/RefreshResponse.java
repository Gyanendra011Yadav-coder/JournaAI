package ai.journa.prcontrol.dto;

import java.time.Instant;

public class RefreshResponse {
    private String status;
    private boolean staleCache;
    private Instant lastRefreshedAt;
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

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
