package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Journalist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalistRepository extends JpaRepository<Journalist, Long> {
}
