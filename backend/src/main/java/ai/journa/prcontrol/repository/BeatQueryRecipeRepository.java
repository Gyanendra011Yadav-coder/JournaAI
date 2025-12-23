package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.BeatQueryRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatQueryRecipeRepository extends JpaRepository<BeatQueryRecipe, Long> {
  List<BeatQueryRecipe> findByBeatId(Long beatId);
}
