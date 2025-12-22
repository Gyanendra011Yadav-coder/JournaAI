package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
