package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.AdminApprovalQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminApprovalQueueRepository extends JpaRepository<AdminApprovalQueueItem, Long> {
}
