package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.LlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmProviderRepository extends JpaRepository<LlmProvider, Long> {
}
