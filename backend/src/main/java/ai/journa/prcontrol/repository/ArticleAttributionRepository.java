package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.ArticleAttribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleAttributionRepository extends JpaRepository<ArticleAttribution, Long> {
}
