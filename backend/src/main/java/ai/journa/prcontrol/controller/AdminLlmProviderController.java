package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.LlmProvider;
import ai.journa.prcontrol.dto.LlmProviderKeyRequest;
import ai.journa.prcontrol.dto.LlmProviderResponse;
import ai.journa.prcontrol.dto.LlmProviderUpdateRequest;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.LlmProviderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/llm/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLlmProviderController {
  private final LlmProviderService llmProviderService;
  private final CurrentUserService currentUserService;

  public AdminLlmProviderController(LlmProviderService llmProviderService,
                                    CurrentUserService currentUserService) {
    this.llmProviderService = llmProviderService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<LlmProviderResponse> listProviders() {
    return llmProviderService.listProviders().stream()
        .map(this::toResponse)
        .toList();
  }

  @PutMapping("/{id}")
  public LlmProviderResponse updateProvider(@PathVariable Long id,
                                            @Valid @RequestBody LlmProviderUpdateRequest request) {
    LlmProvider updated = llmProviderService.updateProvider(id, request, currentUserService.requireCurrentUser());
    return toResponse(updated);
  }

  @PostMapping("/{id}/key")
  public LlmProviderResponse updateKey(@PathVariable Long id,
                                       @Valid @RequestBody LlmProviderKeyRequest request) {
    LlmProvider updated = llmProviderService.updateKey(id, request, currentUserService.requireCurrentUser());
    return toResponse(updated);
  }

  private LlmProviderResponse toResponse(LlmProvider provider) {
    LlmProviderResponse response = new LlmProviderResponse();
    response.setId(provider.getId());
    response.setName(provider.getName());
    response.setEnabled(provider.isEnabled());
    response.setConfigured(llmProviderService.isConfigured(provider));
    response.setBaseUrl(provider.getBaseUrl());
    response.setAuthType(provider.getAuthType() != null ? provider.getAuthType().name() : null);
    response.setAuthHeaderName(provider.getAuthHeaderName());
    response.setAuthQueryParamName(provider.getAuthQueryParamName());
    response.setModel(provider.getModel());
    response.setRequestTemplateJsonb(provider.getRequestTemplateJsonb());
    response.setResponseJsonpath(provider.getResponseJsonpath());
    response.setTimeoutMs(provider.getTimeoutMs());
    response.setRetryPolicyJsonb(provider.getRetryPolicyJsonb());
    response.setUpdatedAt(provider.getUpdatedAt() != null ? provider.getUpdatedAt().toString() : null);
    response.setUpdatedBy(provider.getUpdatedBy() != null ? provider.getUpdatedBy().getEmail() : null);
    return response;
  }
}
