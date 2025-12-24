package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.IntegrationKeyRequest;
import ai.journa.prcontrol.dto.IntegrationSettingsUpdateRequest;
import ai.journa.prcontrol.repository.IntegrationSettingsRepository;
import ai.journa.prcontrol.config.NewsProviderProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IntegrationSettingsService {
  private final IntegrationSettingsRepository integrationSettingsRepository;
  private final EncryptionService encryptionService;
  private final NewsProviderProperties newsProviderProperties;

  public IntegrationSettingsService(IntegrationSettingsRepository integrationSettingsRepository,
                                    EncryptionService encryptionService,
                                    NewsProviderProperties newsProviderProperties) {
    this.integrationSettingsRepository = integrationSettingsRepository;
    this.encryptionService = encryptionService;
    this.newsProviderProperties = newsProviderProperties;
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

  public String resolveApiKey(IntegrationSettings settings) {
    String encrypted = settings.getApiKeyEncrypted();
    if (encrypted != null && !encrypted.isBlank()) {
      return encryptionService.decrypt(encrypted);
    }
    if (settings.getProviderType() == ProviderType.GNEWS) {
      return newsProviderProperties.getGnews().getDecodedApiKey();
    }
    return null;
  }

  public boolean isConfigured(IntegrationSettings settings) {
    if (settings.getApiKeyEncrypted() != null && !settings.getApiKeyEncrypted().isBlank()) {
      return true;
    }
    if (settings.getProviderType() == ProviderType.GNEWS) {
      String apiKey = newsProviderProperties.getGnews().getDecodedApiKey();
      return apiKey != null && !apiKey.isBlank();
    }
    return false;
  }
}
