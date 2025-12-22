package ai.journa.prcontrol.service.integration.real;

import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;

import java.util.Collections;
import java.util.List;

public class NewsApiProvider implements NewsProvider {
    @Override
    public List<NewsArticle> fetchArticles(String beat, String dateRange, String filters, int page) {
        String apiKey = System.getenv("NEWSAPI_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("NEWSAPI_KEY is not configured");
        }
        return Collections.emptyList();
    }
}
