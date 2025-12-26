package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleJournalist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleJournalistRepository extends JpaRepository<ArticleJournalist, Long> {
  List<ArticleJournalist> findByJournalistId(Long journalistId);

  List<ArticleJournalist> findByArticleId(Long articleId);

  Optional<ArticleJournalist> findFirstByArticleIdOrderByMatchConfidenceDesc(Long articleId);
}
