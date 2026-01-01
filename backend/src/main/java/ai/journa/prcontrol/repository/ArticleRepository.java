package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {
  @EntityGraph(attributePaths = { "beat" })
  Optional<Article> findWithBeatById(Long id);

  Optional<Article> findByUrl(String url);
}
