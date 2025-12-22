package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class RefreshRequest {
    @NotBlank
    private String beat;
    @NotBlank
    private String timeframe;
    private Instant from;
    private Instant to;

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

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }
}
