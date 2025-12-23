package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.AuditLog;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  public void record(User actor, String action, String entityType, Object metadata, String entityId) {
    AuditLog log = new AuditLog();
    log.setActorUser(actor);
    log.setAction(action);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setMetadataJsonb(serialize(metadata));
    auditLogRepository.save(log);
  }

  private String serialize(Object metadata) {
    if (metadata == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
