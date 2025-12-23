package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
  @Query("""
      SELECT a FROM Article a
      WHERE (:beatId IS NULL OR a.beat.id = :beatId)
        AND (:status IS NULL OR a.status = :status)
        AND (:from IS NULL OR a.providerPublishedAtUtc >= :from)
        AND (:to IS NULL OR a.providerPublishedAtUtc <= :to)
      """)
  Page<Article> findFiltered(@Param("beatId") Long beatId,
                             @Param("status") ArticleStatus status,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

  Optional<Article> findByUrl(String url);
}
