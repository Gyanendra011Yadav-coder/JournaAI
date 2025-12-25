package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalistRepository extends JpaRepository<Journalist, Long> {
  Optional<Journalist> findByPublicationDomainIgnoreCaseAndFullNameIgnoreCase(String publicationDomain, String fullName);

  Optional<Journalist> findByLinkedinIgnoreCase(String linkedin);

  List<Journalist> findByVerificationStatus(JournalistVerificationStatus status);
}
