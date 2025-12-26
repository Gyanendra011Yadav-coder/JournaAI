package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.logging.request")
public class RequestLoggingProperties {
  private boolean enabled = true;
  private boolean logStart = false;
  private boolean logSuccess = false;
  private long slowThresholdMs = 1500;
  private List<String> includePaths = new ArrayList<>();
  private List<String> excludePaths = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isLogStart() {
    return logStart;
  }

  public void setLogStart(boolean logStart) {
    this.logStart = logStart;
  }

  public boolean isLogSuccess() {
    return logSuccess;
  }

  public void setLogSuccess(boolean logSuccess) {
    this.logSuccess = logSuccess;
  }

  public long getSlowThresholdMs() {
    return slowThresholdMs;
  }

  public void setSlowThresholdMs(long slowThresholdMs) {
    this.slowThresholdMs = slowThresholdMs;
  }

  public List<String> getIncludePaths() {
    return includePaths;
  }

  public void setIncludePaths(List<String> includePaths) {
    this.includePaths = includePaths;
  }

  public List<String> getExcludePaths() {
    return excludePaths;
  }

  public void setExcludePaths(List<String> excludePaths) {
    this.excludePaths = excludePaths;
  }
}
