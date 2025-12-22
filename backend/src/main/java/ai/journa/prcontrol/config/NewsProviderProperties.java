package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.news")
public class NewsProviderProperties {
    private int ttlMinutes = 15;
    private int failureThreshold = 3;
    private int circuitMinutes = 30;
    private int searchesPerMinute = 30;
    private Gnews gnews = new Gnews();
    private Rss rss = new Rss();

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getCircuitMinutes() {
        return circuitMinutes;
    }

    public void setCircuitMinutes(int circuitMinutes) {
        this.circuitMinutes = circuitMinutes;
    }

    public int getSearchesPerMinute() {
        return searchesPerMinute;
    }

    public void setSearchesPerMinute(int searchesPerMinute) {
        this.searchesPerMinute = searchesPerMinute;
    }

    public Gnews getGnews() {
        return gnews;
    }

    public void setGnews(Gnews gnews) {
        this.gnews = gnews;
    }

    public Rss getRss() {
        return rss;
    }

    public void setRss(Rss rss) {
        this.rss = rss;
    }

    public static class Gnews {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Rss {
        private Map<String, List<String>> feeds;

        public Map<String, List<String>> getFeeds() {
            return feeds;
        }

        public void setFeeds(Map<String, List<String>> feeds) {
            this.feeds = feeds;
        }
    }
}
