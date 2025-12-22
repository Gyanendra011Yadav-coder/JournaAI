package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.AuditLog;
import ai.journa.prcontrol.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actor, String action, String entity, String metadata) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntity(entity);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }
}
