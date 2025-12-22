package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.service.integration.model.NewsArticle;

import java.util.List;

public interface NewsProvider {
    List<NewsArticle> fetchArticles(String beat, String dateRange, String filters, int page);
}
