package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
