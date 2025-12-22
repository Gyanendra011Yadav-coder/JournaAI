package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private NewsFetchStateRepository newsFetchStateRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private ai.journa.prcontrol.config.NewsProviderProperties newsProviderProperties;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void searchArticlesReturnsCachedResults() {
        Article article = new Article();
        article.setHeadline("Cached headline");
        when(newsProviderProperties.getSearchesPerMinute()).thenReturn(30);
        when(articleRepository.findDistinctByBeats_NameIgnoreCaseAndPublishedAtAfterOrderByPublishedAtDesc(
                any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(article)));

        var page = articleService.searchArticles("user@example.com", "Taxation", "24h", Instant.now().minusSeconds(3600), 0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getHeadline()).isEqualTo("Cached headline");
    }
}
