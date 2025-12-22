package ai.journa.prcontrol.service.integration.real;

import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.model.NewsArticle;
import ai.journa.prcontrol.service.integration.model.NewsFetchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GNewsProvider implements NewsProvider {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GNewsProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public List<NewsArticle> fetchArticles(NewsFetchRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GNews API key not configured");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://gnews.io/api/v4/search")
                .queryParam("q", request.getBeat())
                .queryParam("max", request.getSize())
                .queryParam("lang", "en")
                .queryParam("token", apiKey);
        if (request.getFrom() != null) {
            builder.queryParam("from", request.getFrom().toString());
        }
        if (request.getTo() != null) {
            builder.queryParam("to", request.getTo().toString());
        }
        String url = builder.toUriString();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            List<NewsArticle> articles = new ArrayList<>();
            for (JsonNode node : root.path("articles")) {
                NewsArticle article = new NewsArticle();
                article.setHeadline(node.path("title").asText());
                article.setSource(node.path("source").path("name").asText());
                article.setAuthor(node.path("source").path("name").asText());
                String link = node.path("url").asText();
                article.setUrl(link);
                article.setCanonicalUrl(link);
                article.setPublishedAt(parseInstant(node.path("publishedAt").asText()));
                article.setDescription(node.path("description").asText());
                article.setRawPayload(node.toString());
                article.setProvider("gnews");
                articles.add(article);
            }
            return articles;
        } catch (RestClientException | RuntimeException ex) {
            throw new IllegalStateException("GNews API request failed", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("GNews parse failed", ex);
        }
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return Instant.now();
        }
    }
}
