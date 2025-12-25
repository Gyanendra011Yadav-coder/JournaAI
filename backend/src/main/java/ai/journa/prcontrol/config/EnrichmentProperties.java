package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.enrichment")
public class EnrichmentProperties {
  private int htmlCacheMinutes = 60;
  private int perDomainDelayMs = 1500;
  private int requestTimeoutSeconds = 10;
  private Scheduled scheduled = new Scheduled();

  public int getHtmlCacheMinutes() {
    return htmlCacheMinutes;
  }

  public void setHtmlCacheMinutes(int htmlCacheMinutes) {
    this.htmlCacheMinutes = htmlCacheMinutes;
  }

  public int getPerDomainDelayMs() {
    return perDomainDelayMs;
  }

  public void setPerDomainDelayMs(int perDomainDelayMs) {
    this.perDomainDelayMs = perDomainDelayMs;
  }

  public int getRequestTimeoutSeconds() {
    return requestTimeoutSeconds;
  }

  public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
    this.requestTimeoutSeconds = requestTimeoutSeconds;
  }

  public Scheduled getScheduled() {
    return scheduled;
  }

  public void setScheduled(Scheduled scheduled) {
    this.scheduled = scheduled;
  }

  public static class Scheduled {
    private boolean enabled = false;
    private long fixedDelayMs = 600000;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public long getFixedDelayMs() {
      return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
      this.fixedDelayMs = fixedDelayMs;
    }
  }
}
