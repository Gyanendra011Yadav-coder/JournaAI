package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.Min;

import java.util.List;

public class ProfileUpdateRequest {
  private List<String> preferredCountries;
  private List<String> preferredLangs;
  private List<Long> beatIds;
  private List<String> clientKeywords;
  private List<String> excludeKeywords;
  private String defaultSidebarMode;

  @Min(0)
  private Integer clientLensRatio;

  @Min(0)
  private Integer trendingLocalRatio;

  public List<String> getPreferredCountries() {
    return preferredCountries;
  }

  public void setPreferredCountries(List<String> preferredCountries) {
    this.preferredCountries = preferredCountries;
  }

  public List<String> getPreferredLangs() {
    return preferredLangs;
  }

  public void setPreferredLangs(List<String> preferredLangs) {
    this.preferredLangs = preferredLangs;
  }

  public List<Long> getBeatIds() {
    return beatIds;
  }

  public void setBeatIds(List<Long> beatIds) {
    this.beatIds = beatIds;
  }

  public List<String> getClientKeywords() {
    return clientKeywords;
  }

  public void setClientKeywords(List<String> clientKeywords) {
    this.clientKeywords = clientKeywords;
  }

  public List<String> getExcludeKeywords() {
    return excludeKeywords;
  }

  public void setExcludeKeywords(List<String> excludeKeywords) {
    this.excludeKeywords = excludeKeywords;
  }

  public String getDefaultSidebarMode() {
    return defaultSidebarMode;
  }

  public void setDefaultSidebarMode(String defaultSidebarMode) {
    this.defaultSidebarMode = defaultSidebarMode;
  }

  public Integer getClientLensRatio() {
    return clientLensRatio;
  }

  public void setClientLensRatio(Integer clientLensRatio) {
    this.clientLensRatio = clientLensRatio;
  }

  public Integer getTrendingLocalRatio() {
    return trendingLocalRatio;
  }

  public void setTrendingLocalRatio(Integer trendingLocalRatio) {
    this.trendingLocalRatio = trendingLocalRatio;
  }
}
