package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ImportJobRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRowRepository extends JpaRepository<ImportJobRow, Long> {
  List<ImportJobRow> findByJobId(Long jobId);
}
