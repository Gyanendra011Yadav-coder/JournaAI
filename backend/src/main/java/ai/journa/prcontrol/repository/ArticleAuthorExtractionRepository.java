package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.AuthorExtractionMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleAuthorExtractionRepository extends JpaRepository<ArticleAuthorExtraction, Long> {
  Optional<ArticleAuthorExtraction> findFirstByArticleIdOrderByExtractedAtDesc(Long articleId);

  Optional<ArticleAuthorExtraction> findFirstByArticleIdAndContentHashOrderByExtractedAtDesc(Long articleId, String contentHash);

  Optional<ArticleAuthorExtraction> findFirstByArticleIdAndContentHashAndMethodOrderByExtractedAtDesc(
      Long articleId,
      String contentHash,
      AuthorExtractionMethod method
  );
}
