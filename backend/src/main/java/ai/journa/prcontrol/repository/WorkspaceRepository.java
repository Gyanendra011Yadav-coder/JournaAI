package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
