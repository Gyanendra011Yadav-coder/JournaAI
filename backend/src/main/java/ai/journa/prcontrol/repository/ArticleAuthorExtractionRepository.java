package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleAuthorExtractionRepository extends JpaRepository<ArticleAuthorExtraction, Long> {
  Optional<ArticleAuthorExtraction> findFirstByArticleIdOrderByExtractedAtDesc(Long articleId);
}
