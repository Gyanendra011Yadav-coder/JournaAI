package ai.journa.prcontrol.seed;

import ai.journa.prcontrol.config.LlmProperties;
import ai.journa.prcontrol.domain.LlmProvider;
import ai.journa.prcontrol.repository.LlmProviderRepository;
import ai.journa.prcontrol.service.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class LlmProviderSeeder implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(LlmProviderSeeder.class);

  private final LlmProviderRepository llmProviderRepository;
  private final LlmProperties llmProperties;
  private final EncryptionService encryptionService;
  private final ObjectMapper objectMapper;

  public LlmProviderSeeder(LlmProviderRepository llmProviderRepository,
                           LlmProperties llmProperties,
                           EncryptionService encryptionService,
                           ObjectMapper objectMapper) {
    this.llmProviderRepository = llmProviderRepository;
    this.llmProperties = llmProperties;
    this.encryptionService = encryptionService;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!llmProperties.isEnabled() || !llmProperties.getSeed().isEnabled()) {
      logger.info("LLM provider seeding disabled; skipping.");
      return;
    }

    List<LlmProperties.Provider> providers = llmProperties.getProviders();
    if (providers == null || providers.isEmpty()) {
      logger.info("No LLM providers configured; skipping seeding.");
      return;
    }

    int created = 0;
    int updated = 0;
    int skipped = 0;

    for (LlmProperties.Provider provider : providers) {
      if (provider == null || provider.getName() == null || provider.getName().isBlank()) {
        continue;
      }
      if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
        logger.warn("Skipping LLM provider '{}' because baseUrl is missing.", provider.getName());
        continue;
      }
      if (provider.getRequestTemplate() == null || provider.getRequestTemplate().isEmpty()) {
        logger.warn("Skipping LLM provider '{}' because requestTemplate is missing.", provider.getName());
        continue;
      }
      if (provider.getResponseJsonpath() == null || provider.getResponseJsonpath().isBlank()) {
        logger.warn("Skipping LLM provider '{}' because responseJsonpath is missing.", provider.getName());
        continue;
      }

      Optional<LlmProvider> existing = llmProviderRepository.findByNameIgnoreCase(provider.getName().trim());
      if (existing.isPresent() && !llmProperties.getSeed().isUpdateExisting()) {
        skipped++;
        continue;
      }

      LlmProvider entity = existing.orElseGet(LlmProvider::new);
      if (entity.getId() == null) {
        entity.setCreatedAt(Instant.now());
      }

      entity.setName(provider.getName().trim());
      entity.setEnabled(provider.isEnabled());
      entity.setBaseUrl(provider.getBaseUrl().trim());
      entity.setAuthType(provider.getAuthType());
      entity.setAuthHeaderName(provider.getAuthHeaderName());
      entity.setAuthQueryParamName(provider.getAuthQueryParamName());
      if (provider.getModel() != null && !provider.getModel().isBlank()) {
        if (shouldOverrideModel(existing, provider, entity)) {
          entity.setModel(provider.getModel().trim());
        }
      }

      String decodedKey = provider.getDecodedApiKey();
      if (decodedKey != null && !decodedKey.isBlank()) {
        entity.setApiKeyEncrypted(encryptionService.encrypt(decodedKey));
      }

      entity.setRequestTemplateJsonb(toJson(provider.getRequestTemplate()));
      entity.setResponseJsonpath(provider.getResponseJsonpath().trim());
      if (provider.getTimeoutMs() != null) {
        entity.setTimeoutMs(provider.getTimeoutMs());
      }
      if (provider.getRetryPolicy() != null && !provider.getRetryPolicy().isEmpty()) {
        entity.setRetryPolicyJsonb(toJson(provider.getRetryPolicy()));
      }

      entity.setUpdatedAt(Instant.now());
      llmProviderRepository.save(entity);

      if (existing.isPresent()) {
        updated++;
      } else {
        created++;
      }
    }

    logger.info("LLM provider seeding complete. Created: {}, Updated: {}, Skipped: {}.",
        created, updated, skipped);
  }

  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize LLM provider config", ex);
    }
  }

  private boolean shouldOverrideModel(Optional<LlmProvider> existing,
                                      LlmProperties.Provider provider,
                                      LlmProvider entity) {
    if (existing.isEmpty()) {
      return true;
    }
    if (entity.getModel() == null || entity.getModel().isBlank()) {
      return true;
    }
    String name = provider.getName();
    if (name != null && name.trim().equalsIgnoreCase("gemini")) {
      return false;
    }
    return true;
  }
}
