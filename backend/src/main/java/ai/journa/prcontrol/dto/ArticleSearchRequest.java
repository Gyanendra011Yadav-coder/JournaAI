package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public class ArticleSearchRequest {
    @NotBlank
    private String beat;

    @NotBlank
    private String timeframe;

    private String filters;

    private int page = 1;

    public String getBeat() {
        return beat;
    }

    public void setBeat(String beat) {
        this.beat = beat;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
