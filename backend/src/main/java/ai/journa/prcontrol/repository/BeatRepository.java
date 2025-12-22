package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Beat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatRepository extends JpaRepository<Beat, Long> {
}
