package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.EnrichmentTask;
import ai.journa.prcontrol.domain.EnrichmentTaskStatus;
import ai.journa.prcontrol.domain.EnrichmentTaskType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EnrichmentTaskRepository extends JpaRepository<EnrichmentTask, Long> {
  @EntityGraph(attributePaths = { "article", "journalist" })
  Optional<EnrichmentTask> findWithArticleAndJournalistById(Long id);

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

  Optional<EnrichmentTask> findTopByArticleIdAndStatusInOrderByCreatedAtDesc(
      Long articleId,
      List<EnrichmentTaskStatus> statuses
  );

  Optional<EnrichmentTask> findTopByJournalistIdAndStatusInOrderByCreatedAtDesc(
      Long journalistId,
      List<EnrichmentTaskStatus> statuses
  );

  List<EnrichmentTask> findByJournalistIdOrderByCreatedAtDesc(Long journalistId);

  List<EnrichmentTask> findByStatusAndJournalistIdOrderByCreatedAtDesc(
      EnrichmentTaskStatus status,
      Long journalistId
  );

  List<EnrichmentTask> findByJournalistIdAndTaskTypeOrderByCreatedAtDesc(
      Long journalistId,
      EnrichmentTaskType taskType
  );

  List<EnrichmentTask> findByStatusAndJournalistIdAndTaskTypeOrderByCreatedAtDesc(
      EnrichmentTaskStatus status,
      Long journalistId,
      EnrichmentTaskType taskType
  );

  Optional<EnrichmentTask> findTopByArticleIdAndTaskTypeOrderByCreatedAtDesc(
      Long articleId,
      EnrichmentTaskType taskType
  );

  List<EnrichmentTask> findByArticleIdAndStatusIn(Long articleId, List<EnrichmentTaskStatus> statuses);
}
