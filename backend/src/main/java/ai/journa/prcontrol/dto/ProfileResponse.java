package ai.journa.prcontrol.dto;

import java.util.List;

public class ProfileResponse {
  private String email;
  private List<String> preferredCountries;
  private List<String> preferredLangs;
  private List<Long> beatIds;
  private List<String> clientKeywords;
  private List<String> excludeKeywords;
  private String defaultSidebarMode;
  private int clientLensRatio;
  private int trendingLocalRatio;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

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

  public int getClientLensRatio() {
    return clientLensRatio;
  }

  public void setClientLensRatio(int clientLensRatio) {
    this.clientLensRatio = clientLensRatio;
  }

  public int getTrendingLocalRatio() {
    return trendingLocalRatio;
  }

  public void setTrendingLocalRatio(int trendingLocalRatio) {
    this.trendingLocalRatio = trendingLocalRatio;
  }
}
