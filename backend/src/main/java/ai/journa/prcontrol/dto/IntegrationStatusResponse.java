package ai.journa.prcontrol.dto;

import java.util.Map;

public class IntegrationStatusResponse {
    private boolean gnewsEnabled;
    private int rssFeedCount;
    private Map<String, String> circuitStatus;

    public boolean isGnewsEnabled() {
        return gnewsEnabled;
    }

    public void setGnewsEnabled(boolean gnewsEnabled) {
        this.gnewsEnabled = gnewsEnabled;
    }

    public int getRssFeedCount() {
        return rssFeedCount;
    }

    public void setRssFeedCount(int rssFeedCount) {
        this.rssFeedCount = rssFeedCount;
    }

    public Map<String, String> getCircuitStatus() {
        return circuitStatus;
    }

    public void setCircuitStatus(Map<String, String> circuitStatus) {
        this.circuitStatus = circuitStatus;
    }
}
