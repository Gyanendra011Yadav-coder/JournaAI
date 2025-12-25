package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public class ClientAliasRequest {
  @NotBlank
  private String alias;

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }
}
