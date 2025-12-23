package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.NewsFetchState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsFetchStateRepository extends JpaRepository<NewsFetchState, Long> {
  Optional<NewsFetchState> findByBeatId(Long beatId);
}
