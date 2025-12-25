package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.UserClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClientRepository extends JpaRepository<UserClient, Long> {
  List<UserClient> findByUser_Id(Long userId);
}
