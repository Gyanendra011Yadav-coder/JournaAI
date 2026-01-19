package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.journalist-cleanup")
public class JournalistCleanupProperties {
  private boolean enabled = true;
  private long fixedDelayMs = 600000;
  private int batchSize = 200;

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

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
}
