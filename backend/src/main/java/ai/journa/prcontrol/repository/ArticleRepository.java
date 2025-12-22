package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByCanonicalUrl(String canonicalUrl);

    Page<Article> findDistinctByBeats_NameIgnoreCaseAndPublishedAtAfterOrderByPublishedAtDesc(String beat,
                                                                                             Instant from,
                                                                                             Pageable pageable);

    Page<Article> findByPublishedAtAfterOrderByPublishedAtDesc(Instant from, Pageable pageable);
}
