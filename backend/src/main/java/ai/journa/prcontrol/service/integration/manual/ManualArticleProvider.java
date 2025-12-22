package ai.journa.prcontrol.service.integration.manual;

import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.service.integration.model.NewsArticle;

import java.time.Instant;

public class ManualArticleProvider {
    public NewsArticle createArticle(ManualArticleRequest request) {
        NewsArticle article = new NewsArticle();
        article.setHeadline(request.getHeadline());
        article.setSource(request.getSource());
        article.setAuthor(request.getAuthor());
        article.setUrl(request.getUrl());
        article.setCanonicalUrl(request.getUrl());
        article.setPublishedAt(request.getPublishedAt() != null ? request.getPublishedAt() : Instant.now());
        article.setDescription(request.getSummary());
        article.setProvider("manual");
        article.setRawPayload("{}");
        return article;
    }
}
