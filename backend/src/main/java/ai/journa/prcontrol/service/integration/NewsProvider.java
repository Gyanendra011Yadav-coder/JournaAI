package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.integration.model.NewsFetchRequest;

import java.util.List;

public interface NewsProvider {
    List<NewsArticle> fetchArticles(NewsFetchRequest request);
}
