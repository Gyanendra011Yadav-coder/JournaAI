package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
}
