package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.config.SearchProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleSearchService {
  private static final Logger logger = LoggerFactory.getLogger(GoogleSearchService.class);
  private static final int MAX_RESULTS = 10;

  private final SearchProviderProperties searchProperties;
  private final ObjectMapper objectMapper;

  public GoogleSearchService(SearchProviderProperties searchProperties, ObjectMapper objectMapper) {
    this.searchProperties = searchProperties;
    this.objectMapper = objectMapper;
  }

  public List<SearchResult> search(String query,
                                   String apiKey,
                                   String searchEngineId,
                                   int requestedCount,
                                   String lang,
                                   String country) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    if (apiKey == null || apiKey.isBlank() || searchEngineId == null || searchEngineId.isBlank()) {
      return List.of();
    }
    int count = resolveCount(requestedCount);
    try {
      UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchProperties.getGoogle().getBaseUrl())
          .queryParam("key", apiKey)
          .queryParam("cx", searchEngineId)
          .queryParam("q", query)
          .queryParam("num", count);
      if (lang != null && !lang.isBlank()) {
        builder.queryParam("lr", "lang_" + lang.toLowerCase(Locale.ROOT));
      }
      if (country != null && !country.isBlank()) {
        builder.queryParam("gl", country.toLowerCase(Locale.ROOT));
      }
      URI uri = builder.build().encode().toUri();
      URI logUri = buildLogUri(query, searchEngineId, count, lang, country);
      logger.info("Google CSE request uri={} query={}", logUri, query);
      RestClient client = buildRestClient();
      Instant start = Instant.now();
      String response = client.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
      long durationMs = Duration.between(start, Instant.now()).toMillis();
      if (response == null || response.isBlank()) {
        logger.warn("Google CSE returned empty response query={} durationMs={}", query, durationMs);
        return List.of();
      }
      List<SearchResult> results = parseResults(response);
      logger.info("Google CSE response query={} results={} durationMs={}",
          query,
          results.size(),
          durationMs);
      if (!results.isEmpty()) {
        List<String> topUrls = results.stream().limit(3).map(SearchResult::url).toList();
        logger.info("Google CSE top results query={} urls={}", query, topUrls);
      }
      return results;
    } catch (RestClientResponseException ex) {
      logger.warn("Google CSE failed status={} query={} message={}",
          ex.getRawStatusCode(),
          query,
          ex.getMessage());
      return List.of();
    } catch (Exception ex) {
      logger.warn("Google CSE failed query={} reason={}", query, ex.getMessage());
      return List.of();
    }
  }

  private int resolveCount(int requestedCount) {
    int configured = searchProperties.getGoogle().getMaxResults();
    int count = requestedCount > 0 ? requestedCount : configured;
    if (configured > 0) {
      count = Math.min(count, configured);
    }
    return Math.max(1, Math.min(count, MAX_RESULTS));
  }

  private List<SearchResult> parseResults(String response) {
    try {
      JsonNode root = objectMapper.readTree(response);
      JsonNode items = root.path("items");
      if (!items.isArray()) {
        return Collections.emptyList();
      }
      List<SearchResult> results = new ArrayList<>();
      for (JsonNode node : items) {
        String url = node.path("link").asText(null);
        if (url == null || url.isBlank()) {
          continue;
        }
        String title = node.path("title").asText(null);
        String snippet = node.path("snippet").asText(null);
        results.add(new SearchResult(url, title, snippet));
      }
      return results;
    } catch (Exception ex) {
      logger.warn("Google CSE parse failed reason={}", ex.getMessage());
      return List.of();
    }
  }

  private RestClient buildRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    int timeoutMs = searchProperties.getGoogle().getRequestTimeoutSeconds() * 1000;
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    return RestClient.builder().requestFactory(factory).build();
  }

  private URI buildLogUri(String query, String searchEngineId, int count, String lang, String country) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchProperties.getGoogle().getBaseUrl())
        .queryParam("key", "REDACTED")
        .queryParam("cx", searchEngineId)
        .queryParam("q", query)
        .queryParam("num", count);
    if (lang != null && !lang.isBlank()) {
      builder.queryParam("lr", "lang_" + lang.toLowerCase(Locale.ROOT));
    }
    if (country != null && !country.isBlank()) {
      builder.queryParam("gl", country.toLowerCase(Locale.ROOT));
    }
    return builder.build().encode().toUri();
  }

  public record SearchResult(String url, String title, String snippet) {
  }
}
