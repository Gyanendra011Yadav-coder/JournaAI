package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Beat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeatRepository extends JpaRepository<Beat, Long> {
  Optional<Beat> findBySlug(String slug);
}
