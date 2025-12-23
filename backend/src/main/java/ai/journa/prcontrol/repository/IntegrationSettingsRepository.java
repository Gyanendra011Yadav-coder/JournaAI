package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationSettingsRepository extends JpaRepository<IntegrationSettings, Long> {
  Optional<IntegrationSettings> findByProviderType(ProviderType providerType);
}
