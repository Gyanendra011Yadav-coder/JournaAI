package ai.journa.prcontrol.service.mock;

import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MockNewsProvider implements NewsProvider {
    @Override
    public List<NewsArticle> fetchArticles(String beat, String dateRange, String filters, int page) {
        List<NewsArticle> articles = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            NewsArticle article = new NewsArticle();
            article.setHeadline("" + beat + " Briefing " + i);
            article.setSource("Mock Newswire");
            article.setAuthor("Staff Writer");
            article.setUrl("https://news.local/" + beat + "/" + i);
            article.setPublishedAt(Instant.now().minusSeconds(3600L * i));
            article.setDescription("A quick overview of " + beat + " trends. Key changes and analysis.");
            article.setRawPayload("{\"mock\":true,\"beat\":\"" + beat + "\"}");
            articles.add(article);
        }
        return articles;
    }
}
