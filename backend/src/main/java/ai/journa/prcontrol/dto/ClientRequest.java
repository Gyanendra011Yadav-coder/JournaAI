package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ClientRequest {
  @NotBlank
  private String displayName;
  private String shortName;
  private List<String> aliases;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }
}
