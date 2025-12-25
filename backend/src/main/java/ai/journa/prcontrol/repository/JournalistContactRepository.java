package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.JournalistContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalistContactRepository extends JpaRepository<JournalistContact, Long> {
  List<JournalistContact> findByJournalistId(Long journalistId);

  JournalistContact findFirstByEmailIgnoreCase(String email);
}
