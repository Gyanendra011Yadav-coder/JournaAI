package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.JournalistEnrichmentReview;
import ai.journa.prcontrol.domain.JournalistEnrichmentReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalistEnrichmentReviewRepository extends JpaRepository<JournalistEnrichmentReview, Long> {
  List<JournalistEnrichmentReview> findByStatusOrderByCreatedAtDesc(JournalistEnrichmentReviewStatus status);

  Optional<JournalistEnrichmentReview> findTopByJournalistIdAndStatusOrderByCreatedAtDesc(
      Long journalistId,
      JournalistEnrichmentReviewStatus status
  );
}
