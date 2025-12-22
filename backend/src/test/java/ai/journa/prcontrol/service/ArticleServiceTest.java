package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.dto.ArticleSearchRequest;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.SavedSearchRepository;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.summarizer.Summarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @Mock
    private NewsProvider newsProvider;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private SavedSearchRepository savedSearchRepository;
    @Mock
    private Summarizer summarizer;
    @Mock
    private AuditService auditService;
    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void searchArticlesSummarizesAndPersists() {
        ArticleSearchRequest request = new ArticleSearchRequest();
        request.setBeat("Taxation");
        request.setTimeframe("24h");

        NewsArticle article = new NewsArticle();
        article.setHeadline("Tax update");
        article.setDescription("First sentence. Second sentence.");
        article.setPublishedAt(Instant.now());

        when(newsProvider.fetchArticles(any(), any(), any(), any(Integer.class))).thenReturn(List.of(article));
        when(summarizer.summarize(any())).thenReturn("First sentence.");
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Article> results = articleService.searchArticles("user@example.com", request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSummary()).isEqualTo("First sentence.");
    }
}
