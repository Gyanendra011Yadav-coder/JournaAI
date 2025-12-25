package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.UserClientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClientAliasRepository extends JpaRepository<UserClientAlias, Long> {
  List<UserClientAlias> findByClientId(Long clientId);
}
