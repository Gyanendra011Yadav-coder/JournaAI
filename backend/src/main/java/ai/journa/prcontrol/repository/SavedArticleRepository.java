package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.SavedArticle;
import ai.journa.prcontrol.domain.SavedArticleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedArticleRepository extends JpaRepository<SavedArticle, SavedArticleId> {
  List<SavedArticle> findByUser_Id(Long userId);
}
