package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.domain.ProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GNewsProvider implements NewsProvider {
  private static final Logger logger = LoggerFactory.getLogger(GNewsProvider.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final NewsProviderProperties properties;

  public GNewsProvider(RestTemplate restTemplate, ObjectMapper objectMapper, NewsProviderProperties properties) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public ProviderType type() {
    return ProviderType.GNEWS;
  }

  @Override
  public FetchResult fetch(FetchRequest request, String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new ProviderException("GNews API key is missing", 401, false);
    }
    try {
      return executeRequest(request, apiKey, true);
    } catch (ProviderException ex) {
      if (ex.getStatusCode() == 401) {
        return executeRequest(request, apiKey, false);
      }
      throw ex;
    }
  }

  private FetchResult executeRequest(FetchRequest request, String apiKey, boolean useHeader) {
    String endpoint = request.getEndpointType() == EndpointType.TOP_HEADLINES ? "/top-headlines" : "/search";
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getGnews().getBaseUrl() + endpoint);

    if (request.getEndpointType() == EndpointType.TOP_HEADLINES) {
      if (request.getCategory() != null) {
        builder.queryParam("category", request.getCategory());
      }
      if (request.getQuery() != null && !request.getQuery().isBlank()) {
        builder.queryParam("q", request.getQuery());
      }
    } else {
      if (request.getQuery() == null || request.getQuery().isBlank()) {
        throw new ProviderException("GNews search requires a query", 400, false);
      }
      builder.queryParam("q", request.getQuery());
    }

    if (request.getLang() != null) {
      builder.queryParam("lang", request.getLang());
    }
    if (request.getCountry() != null) {
      builder.queryParam("country", request.getCountry());
    }
    if (request.getInFields() != null) {
      builder.queryParam("in", request.getInFields());
    }
    if (request.getNullableFields() != null) {
      builder.queryParam("nullable", request.getNullableFields());
    }
    if (request.getMax() != null) {
      builder.queryParam("max", request.getMax());
    }
    if (request.getPage() != null) {
      builder.queryParam("page", request.getPage());
    }
    if (request.getSort() != null) {
      builder.queryParam("sortby", request.getSort());
    }
    if (request.getFrom() != null) {
      builder.queryParam("from", request.getFrom().toString());
    }
    if (request.getTo() != null) {
      builder.queryParam("to", request.getTo().toString());
    }
    if (request.getTruncate() != null) {
      builder.queryParam("truncate", request.getTruncate());
    }

    if (!useHeader) {
      builder.queryParam("apikey", apiKey);
    }

    String requestUrl = builder.toUriString();
    logger.info("GNews request url={} headerAuth={}", sanitizeUrl(requestUrl), useHeader);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    if (useHeader) {
      headers.set("X-Api-Key", apiKey);
    }

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
    return parseResponse(response.getBody());
    } catch (HttpStatusCodeException ex) {
      int status = ex.getStatusCode().value();
      boolean retryable = status == 429 || status >= 500;
      throw new ProviderException(extractError(ex.getResponseBodyAsString()), status, retryable);
    } catch (Exception ex) {
      throw new ProviderException("Unexpected GNews error", ex, 500, true);
    }
  }

  private FetchResult parseResponse(String body) throws Exception {
    JsonNode root = objectMapper.readTree(body);
    int total = root.path("totalArticles").asInt();
    List<ProviderArticle> articles = new ArrayList<>();
    for (JsonNode node : root.path("articles")) {
      ProviderArticle article = new ProviderArticle();
      article.setId(node.path("id").asText(null));
      article.setTitle(node.path("title").asText());
      article.setDescription(node.path("description").asText(null));
      article.setContent(node.path("content").asText(null));
      article.setUrl(node.path("url").asText());
      article.setImage(node.path("image").asText(null));
      article.setPublishedAt(parseInstant(node.path("publishedAt").asText(null)));
      article.setLang(node.path("lang").asText(null));
      JsonNode source = node.path("source");
      article.setSourceId(source.path("id").asText(null));
      article.setSourceName(source.path("name").asText(null));
      article.setSourceUrl(source.path("url").asText(null));
      article.setSourceCountry(source.path("country").asText(null));
      article.setRawPayload(node.toString());
      articles.add(article);
    }
    FetchResult result = new FetchResult(total, articles);
    result.setRawPayload(body);
    return result;
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      return null;
    }
  }

  private String extractError(String body) {
    try {
      JsonNode root = objectMapper.readTree(body);
      if (root.has("errors") && root.get("errors").isArray()) {
        return root.get("errors").toString();
      }
      if (root.has("message")) {
        return root.get("message").asText();
      }
    } catch (Exception ignored) {
      return body != null ? body : "GNews error";
    }
    return body != null ? body : "GNews error";
  }

  private String sanitizeUrl(String url) {
    if (url == null) {
      return null;
    }
    int idx = url.indexOf("apikey=");
    if (idx == -1) {
      return url;
    }
    int end = url.indexOf('&', idx);
    if (end == -1) {
      end = url.length();
    }
    return url.substring(0, idx) + "apikey=***" + url.substring(end);
  }
}
