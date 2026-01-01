package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.LlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LlmProviderRepository extends JpaRepository<LlmProvider, Long> {
  Optional<LlmProvider> findByNameIgnoreCase(String name);
}
