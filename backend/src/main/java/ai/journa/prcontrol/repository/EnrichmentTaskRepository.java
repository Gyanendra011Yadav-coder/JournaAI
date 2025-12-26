package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.EnrichmentTask;
import ai.journa.prcontrol.domain.EnrichmentTaskStatus;
import ai.journa.prcontrol.domain.EnrichmentTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface EnrichmentTaskRepository extends JpaRepository<EnrichmentTask, Long> {
  List<EnrichmentTask> findTop50ByStatusInAndNextRunAtBeforeOrderByPriorityDescCreatedAtAsc(
      List<EnrichmentTaskStatus> statuses,
      Instant nextRunAt
  );

  List<EnrichmentTask> findByStatus(EnrichmentTaskStatus status);

  List<EnrichmentTask> findByTaskTypeAndStatus(EnrichmentTaskType taskType, EnrichmentTaskStatus status);

  List<EnrichmentTask> findByStatusAndNextRunAtBeforeOrderByPriorityDescCreatedAtDesc(
      EnrichmentTaskStatus status,
      Instant nextRunAt,
      Pageable pageable
  );

  List<EnrichmentTask> findByStatusAndNextRunAtBeforeOrderByPriorityDescCreatedAtAsc(
      EnrichmentTaskStatus status,
      Instant nextRunAt,
      Pageable pageable
  );
}
