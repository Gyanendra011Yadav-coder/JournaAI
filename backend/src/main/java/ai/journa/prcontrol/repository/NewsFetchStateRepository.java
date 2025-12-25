package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.NewsFetchState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsFetchStateRepository extends JpaRepository<NewsFetchState, Long> {
  Optional<NewsFetchState> findByKey(String key);
}
