package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.BeatQueryTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatQueryTemplateRepository extends JpaRepository<BeatQueryTemplate, Long> {
  List<BeatQueryTemplate> findByBeatId(Long beatId);
}
