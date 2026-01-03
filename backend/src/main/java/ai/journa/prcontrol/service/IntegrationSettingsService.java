package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.IntegrationKeyRequest;
import ai.journa.prcontrol.dto.IntegrationSettingsUpdateRequest;
import ai.journa.prcontrol.repository.IntegrationSettingsRepository;
import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.config.SearchProviderProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IntegrationSettingsService {
  private final IntegrationSettingsRepository integrationSettingsRepository;
  private final EncryptionService encryptionService;
  private final NewsProviderProperties newsProviderProperties;
  private final SearchProviderProperties searchProviderProperties;

  public IntegrationSettingsService(IntegrationSettingsRepository integrationSettingsRepository,
                                    EncryptionService encryptionService,
                                    NewsProviderProperties newsProviderProperties,
                                    SearchProviderProperties searchProviderProperties) {
    this.integrationSettingsRepository = integrationSettingsRepository;
    this.encryptionService = encryptionService;
    this.newsProviderProperties = newsProviderProperties;
    this.searchProviderProperties = searchProviderProperties;
  }

  public IntegrationSettings getSettings(ProviderType providerType) {
    return integrationSettingsRepository.findByProviderType(providerType)
        .orElseThrow(() -> new IllegalStateException("Integration settings not found"));
  }

  public IntegrationSettings getActiveSettings() {
    return getSettings(ProviderType.GNEWS);
  }

  public IntegrationSettings updateSettings(ProviderType providerType, IntegrationSettingsUpdateRequest request, User actor) {
    IntegrationSettings settings = getSettings(providerType);
    settings.setEnabled(request.getEnabled());
    settings.setDefaultLang(request.getDefaultLang());
    settings.setDefaultCountry(request.getDefaultCountry());
    settings.setRefreshIntervalMinutes(request.getRefreshIntervalMinutes());
    settings.setTtlMinutes(request.getTtlMinutes());
    settings.setMaxPerRequest(request.getMaxPerRequest());
    if (request.getSearchEngineId() != null) {
      String engineId = request.getSearchEngineId().trim();
      settings.setSearchEngineId(engineId.isBlank() ? null : engineId);
    }
    if (request.getAllowedDomains() != null) {
      settings.setAllowedDomains(request.getAllowedDomains());
    }
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
    if (settings.getProviderType() == ProviderType.GOOGLE_CSE) {
      return searchProviderProperties.getGoogle().getDecodedApiKey();
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
    if (settings.getProviderType() == ProviderType.GOOGLE_CSE) {
      String apiKey = searchProviderProperties.getGoogle().getDecodedApiKey();
      String engineId = resolveSearchEngineId(settings);
      return apiKey != null && !apiKey.isBlank() && engineId != null && !engineId.isBlank();
    }
    return false;
  }

  public String resolveSearchEngineId(IntegrationSettings settings) {
    if (settings.getSearchEngineId() != null && !settings.getSearchEngineId().isBlank()) {
      return settings.getSearchEngineId().trim();
    }
    if (settings.getProviderType() == ProviderType.GOOGLE_CSE) {
      return searchProviderProperties.getGoogle().getSearchEngineId();
    }
    return null;
  }
}
