package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.LlmProvider;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.LlmProviderKeyRequest;
import ai.journa.prcontrol.dto.LlmProviderUpdateRequest;
import ai.journa.prcontrol.repository.LlmProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LlmProviderService {
  private final LlmProviderRepository llmProviderRepository;
  private final EncryptionService encryptionService;
  private final ObjectMapper objectMapper;

  public LlmProviderService(LlmProviderRepository llmProviderRepository,
                            EncryptionService encryptionService,
                            ObjectMapper objectMapper) {
    this.llmProviderRepository = llmProviderRepository;
    this.encryptionService = encryptionService;
    this.objectMapper = objectMapper;
  }

  public List<LlmProvider> listProviders() {
    return llmProviderRepository.findAll(Sort.by("name").ascending());
  }

  public LlmProvider updateProvider(Long id, LlmProviderUpdateRequest request, User actor) {
    LlmProvider provider = llmProviderRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("LLM provider not found"));

    provider.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
    provider.setBaseUrl(trimRequired(request.getBaseUrl(), "baseUrl"));
    provider.setAuthType(request.getAuthType());
    provider.setAuthHeaderName(trimOptional(request.getAuthHeaderName()));
    provider.setAuthQueryParamName(trimOptional(request.getAuthQueryParamName()));
    provider.setModel(trimOptional(request.getModel()));
    provider.setRequestTemplateJsonb(normalizeJson(request.getRequestTemplateJsonb(), "requestTemplateJsonb"));
    provider.setResponseJsonpath(trimRequired(request.getResponseJsonpath(), "responseJsonpath"));

    Integer timeoutMs = request.getTimeoutMs();
    if (timeoutMs != null && timeoutMs > 0) {
      provider.setTimeoutMs(timeoutMs);
    }

    String retryPolicyJson = trimOptional(request.getRetryPolicyJsonb());
    if (retryPolicyJson != null && !retryPolicyJson.isBlank()) {
      provider.setRetryPolicyJsonb(normalizeJson(retryPolicyJson, "retryPolicyJsonb"));
    } else {
      provider.setRetryPolicyJsonb(null);
    }

    provider.setUpdatedAt(Instant.now());
    provider.setUpdatedBy(actor);
    return llmProviderRepository.save(provider);
  }

  public LlmProvider updateKey(Long id, LlmProviderKeyRequest request, User actor) {
    LlmProvider provider = llmProviderRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("LLM provider not found"));
    provider.setApiKeyEncrypted(encryptionService.encrypt(request.getApiKey().trim()));
    provider.setUpdatedAt(Instant.now());
    provider.setUpdatedBy(actor);
    return llmProviderRepository.save(provider);
  }

  public boolean isConfigured(LlmProvider provider) {
    String key = provider.getApiKeyEncrypted();
    return key != null && !key.isBlank();
  }

  private String normalizeJson(String payload, String fieldName) {
    try {
      JsonNode node = objectMapper.readTree(payload);
      if (node == null || node.isNull()) {
        throw new IllegalStateException(fieldName + " cannot be null");
      }
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid JSON for " + fieldName, ex);
    }
  }

  private String trimRequired(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(fieldName + " is required");
    }
    return value.trim();
  }

  private String trimOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
