package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.UserKeyword;
import ai.journa.prcontrol.domain.UserKeywordKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {
  List<UserKeyword> findByUser_Id(Long userId);
  List<UserKeyword> findByUser_IdAndKind(Long userId, UserKeywordKind kind);
}
