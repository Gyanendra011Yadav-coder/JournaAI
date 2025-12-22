package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "news_fetch_state")
public class NewsFetchState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beat_id", nullable = false)
    private Beat beat;

    @Column(nullable = false)
    private String timeframe;

    private Instant lastFetchedAt;

    @Column(nullable = false)
    private int failureCount = 0;

    private Instant lastFailureAt;

    private Instant circuitOpenUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Beat getBeat() {
        return beat;
    }

    public void setBeat(Beat beat) {
        this.beat = beat;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Instant getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(Instant lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(Instant lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }

    public Instant getCircuitOpenUntil() {
        return circuitOpenUntil;
    }

    public void setCircuitOpenUntil(Instant circuitOpenUntil) {
        this.circuitOpenUntil = circuitOpenUntil;
    }
}
