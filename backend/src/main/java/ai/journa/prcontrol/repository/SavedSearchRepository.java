package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
}
