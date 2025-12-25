package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.domain.ProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockNewsProvider implements NewsProvider {
  private final ObjectMapper objectMapper;

  public MockNewsProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public ProviderType type() {
    return ProviderType.MOCK;
  }

  @Override
  public FetchResult fetch(FetchRequest request, String apiKey) {
    List<ProviderArticle> articles = new ArrayList<>();
    for (int i = 0; i < Math.min(5, request.getMax() != null ? request.getMax() : 5); i++) {
      ProviderArticle article = new ProviderArticle();
      article.setId(UUID.randomUUID().toString());
      article.setTitle("Mock article " + (i + 1));
      article.setDescription("Mock description for " + request.getQuery());
      article.setContent("Mock content payload");
      article.setUrl("https://mock.example.com/article-" + UUID.randomUUID());
      article.setImage("https://mock.example.com/image.jpg");
      article.setPublishedAt(Instant.now().minusSeconds(i * 3600L));
      article.setLang(request.getLang());
      article.setSourceId("mock-source");
      article.setSourceName("Mock Source");
      article.setSourceUrl("https://mock.example.com");
      article.setSourceCountry(request.getCountry());
      article.setRawPayload(serializePayload(article));
      articles.add(article);
    }
    FetchResult result = new FetchResult(articles.size(), articles);
    result.setRawPayload(serializePayload(articles));
    return result;
  }

  private String serializePayload(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
