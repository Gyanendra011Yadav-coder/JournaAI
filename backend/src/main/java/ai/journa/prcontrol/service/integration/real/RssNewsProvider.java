package ai.journa.prcontrol.service.integration.real;

import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.integration.model.NewsFetchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RssNewsProvider implements NewsProvider {
    private final ObjectMapper objectMapper;
    private final Map<String, List<String>> feeds;

    public RssNewsProvider(ObjectMapper objectMapper, Map<String, List<String>> feeds) {
        this.objectMapper = objectMapper;
        this.feeds = feeds != null ? feeds : Collections.emptyMap();
    }

    @Override
    public List<NewsArticle> fetchArticles(NewsFetchRequest request) {
        List<String> sources = feeds.getOrDefault(request.getBeat(), List.of());
        List<NewsArticle> articles = new ArrayList<>();
        for (String feedUrl : sources) {
            articles.addAll(readFeed(feedUrl));
        }
        return articles;
    }

    private List<NewsArticle> readFeed(String feedUrl) {
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));
            List<NewsArticle> results = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                NewsArticle article = new NewsArticle();
                article.setHeadline(entry.getTitle());
                article.setSource(feed.getTitle());
                article.setAuthor(entry.getAuthor());
                article.setUrl(entry.getLink());
                article.setCanonicalUrl(entry.getLink());
                Instant published = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant()
                        : Instant.now();
                article.setPublishedAt(published);
                article.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : null);
                article.setRawPayload(objectMapper.writeValueAsString(Map.of(
                        "title", entry.getTitle(),
                        "link", entry.getLink(),
                        "source", feed.getTitle()
                )));
                article.setProvider("rss");
                results.add(article);
            }
            return results;
        } catch (Exception ex) {
            throw new IllegalStateException("RSS ingestion failed for " + feedUrl, ex);
        }
    }
}
