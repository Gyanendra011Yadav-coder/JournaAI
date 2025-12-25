package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.NewsCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsCacheRepository extends JpaRepository<NewsCache, Long> {
  Optional<NewsCache> findByCacheKey(String cacheKey);
}
