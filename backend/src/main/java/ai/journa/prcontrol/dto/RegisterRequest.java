package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class RegisterRequest {
  @Email
  @NotBlank
  private String email;

  @NotBlank
  private String password;

  private List<String> preferredCountries;

  private List<String> preferredLangs;

  private List<Long> beatIds;

  private List<String> clientKeywords;

  private List<String> excludeKeywords;

  private List<ClientRequest> clients;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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

  public List<ClientRequest> getClients() {
    return clients;
  }

  public void setClients(List<ClientRequest> clients) {
    this.clients = clients;
  }

  public static class ClientRequest {
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
}
