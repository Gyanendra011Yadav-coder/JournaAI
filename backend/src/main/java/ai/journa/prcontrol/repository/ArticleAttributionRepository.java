package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleAttribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleAttributionRepository extends JpaRepository<ArticleAttribution, Long> {
  Optional<ArticleAttribution> findFirstByArticleIdAndBureauNameIgnoreCase(Long articleId, String bureauName);
}
