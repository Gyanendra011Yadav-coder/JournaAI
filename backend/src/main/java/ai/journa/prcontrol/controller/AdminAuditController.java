package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.AuditLog;
import ai.journa.prcontrol.dto.AuditResponse;
import ai.journa.prcontrol.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
  private final AuditLogRepository auditLogRepository;

  public AdminAuditController(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @GetMapping
  public List<AuditResponse> list(@RequestParam(required = false) String action,
                                  @RequestParam(required = false) String entityType) {
    return auditLogRepository.findAll().stream()
        .filter(log -> action == null || log.getAction().equalsIgnoreCase(action))
        .filter(log -> entityType == null || log.getEntityType().equalsIgnoreCase(entityType))
        .map(this::toResponse)
        .toList();
  }

  private AuditResponse toResponse(AuditLog log) {
    AuditResponse response = new AuditResponse();
    response.setId(log.getId());
    response.setActorEmail(log.getActorUser() != null ? log.getActorUser().getEmail() : null);
    response.setAction(log.getAction());
    response.setEntityType(log.getEntityType());
    response.setEntityId(log.getEntityId());
    response.setCreatedAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
    response.setMetadata(log.getMetadataJsonb());
    return response;
  }
}
