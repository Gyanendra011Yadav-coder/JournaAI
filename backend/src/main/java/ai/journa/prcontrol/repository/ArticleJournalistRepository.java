package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleJournalist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleJournalistRepository extends JpaRepository<ArticleJournalist, Long> {
  List<ArticleJournalist> findByJournalistId(Long journalistId);

  List<ArticleJournalist> findByArticleId(Long articleId);
}
