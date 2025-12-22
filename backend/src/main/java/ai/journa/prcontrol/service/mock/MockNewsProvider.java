package ai.journa.prcontrol.service.mock;

import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.integration.model.NewsFetchRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MockNewsProvider implements NewsProvider {
    @Override
    public List<NewsArticle> fetchArticles(NewsFetchRequest request) {
        List<NewsArticle> articles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            NewsArticle article = new NewsArticle();
            article.setHeadline(request.getBeat() + " mock headline " + (i + 1));
            article.setSource("Mock Wire");
            article.setAuthor("Mock Author");
            article.setUrl("https://example.com/mock-" + request.getBeat() + "-" + i);
            article.setCanonicalUrl(article.getUrl());
            article.setPublishedAt(Instant.now().minusSeconds(i * 3600L));
            article.setDescription("Mock description for " + request.getBeat());
            article.setRawPayload("{}");
            article.setProvider("mock");
            articles.add(article);
        }
        return articles;
    }
}
