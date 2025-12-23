package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.dto.IntegrationKeyRequest;
import ai.journa.prcontrol.dto.IntegrationSettingsResponse;
import ai.journa.prcontrol.dto.IntegrationSettingsUpdateRequest;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.IntegrationSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/integrations/gnews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIntegrationController {
  private final IntegrationSettingsService integrationSettingsService;
  private final CurrentUserService currentUserService;

  public AdminIntegrationController(IntegrationSettingsService integrationSettingsService,
                                    CurrentUserService currentUserService) {
    this.integrationSettingsService = integrationSettingsService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public IntegrationSettingsResponse getSettings() {
    IntegrationSettings settings = integrationSettingsService.getSettings(ProviderType.GNEWS);
    return toResponse(settings);
  }

  @PutMapping
  public IntegrationSettingsResponse update(@Valid @RequestBody IntegrationSettingsUpdateRequest request) {
    IntegrationSettings settings = integrationSettingsService.updateSettings(
        ProviderType.GNEWS,
        request,
        currentUserService.requireCurrentUser()
    );
    return toResponse(settings);
  }

  @PostMapping("/key")
  public IntegrationSettingsResponse updateKey(@Valid @RequestBody IntegrationKeyRequest request) {
    IntegrationSettings settings = integrationSettingsService.updateApiKey(
        ProviderType.GNEWS,
        request,
        currentUserService.requireCurrentUser()
    );
    return toResponse(settings);
  }

  private IntegrationSettingsResponse toResponse(IntegrationSettings settings) {
    IntegrationSettingsResponse response = new IntegrationSettingsResponse();
    response.setProviderType(settings.getProviderType().name());
    response.setEnabled(settings.isEnabled());
    response.setConfigured(integrationSettingsService.isConfigured(settings));
    response.setDefaultLang(settings.getDefaultLang());
    response.setDefaultCountry(settings.getDefaultCountry());
    response.setRefreshIntervalMinutes(settings.getRefreshIntervalMinutes());
    response.setTtlMinutes(settings.getTtlMinutes());
    response.setMaxPerRequest(settings.getMaxPerRequest());
    response.setUpdatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt().toString() : null);
    response.setUpdatedBy(settings.getUpdatedBy() != null ? settings.getUpdatedBy().getEmail() : null);
    return response;
  }
}
