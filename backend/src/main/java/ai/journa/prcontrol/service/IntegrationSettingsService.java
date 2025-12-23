package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.IntegrationKeyRequest;
import ai.journa.prcontrol.dto.IntegrationSettingsUpdateRequest;
import ai.journa.prcontrol.repository.IntegrationSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IntegrationSettingsService {
  private final IntegrationSettingsRepository integrationSettingsRepository;
  private final EncryptionService encryptionService;

  public IntegrationSettingsService(IntegrationSettingsRepository integrationSettingsRepository,
                                    EncryptionService encryptionService) {
    this.integrationSettingsRepository = integrationSettingsRepository;
    this.encryptionService = encryptionService;
  }

  public IntegrationSettings getSettings(ProviderType providerType) {
    return integrationSettingsRepository.findByProviderType(providerType)
        .orElseThrow(() -> new IllegalStateException("Integration settings not found"));
  }

  public IntegrationSettings getActiveSettings() {
    return integrationSettingsRepository.findAll().stream().findFirst()
        .orElseThrow(() -> new IllegalStateException("Integration settings not found"));
  }

  public IntegrationSettings updateSettings(ProviderType providerType, IntegrationSettingsUpdateRequest request, User actor) {
    IntegrationSettings settings = getSettings(providerType);
    settings.setEnabled(request.getEnabled());
    settings.setDefaultLang(request.getDefaultLang());
    settings.setDefaultCountry(request.getDefaultCountry());
    settings.setRefreshIntervalMinutes(request.getRefreshIntervalMinutes());
    settings.setTtlMinutes(request.getTtlMinutes());
    settings.setMaxPerRequest(request.getMaxPerRequest());
    settings.setUpdatedAt(Instant.now());
    settings.setUpdatedBy(actor);
    return integrationSettingsRepository.save(settings);
  }

  public IntegrationSettings updateApiKey(ProviderType providerType, IntegrationKeyRequest request, User actor) {
    IntegrationSettings settings = getSettings(providerType);
    settings.setApiKeyEncrypted(encryptionService.encrypt(request.getApiKey()));
    settings.setUpdatedAt(Instant.now());
    settings.setUpdatedBy(actor);
    return integrationSettingsRepository.save(settings);
  }

  public String decryptApiKey(IntegrationSettings settings) {
    return encryptionService.decrypt(settings.getApiKeyEncrypted());
  }

  public boolean isConfigured(IntegrationSettings settings) {
    return settings.getApiKeyEncrypted() != null && !settings.getApiKeyEncrypted().isBlank();
  }
}
