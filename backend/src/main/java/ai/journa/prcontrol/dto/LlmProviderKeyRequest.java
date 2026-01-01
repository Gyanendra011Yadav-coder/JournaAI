package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public class LlmProviderKeyRequest {
  @NotBlank
  private String apiKey;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
