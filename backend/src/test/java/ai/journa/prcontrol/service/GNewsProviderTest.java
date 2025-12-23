package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.service.integration.FetchRequest;
import ai.journa.prcontrol.service.integration.GNewsProvider;
import ai.journa.prcontrol.service.integration.ProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GNewsProviderTest {
  private RestTemplate restTemplate;
  private MockRestServiceServer server;
  private GNewsProvider provider;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    NewsProviderProperties properties = new NewsProviderProperties();
    properties.getGnews().setBaseUrl("https://gnews.test/api/v4");
    provider = new GNewsProvider(restTemplate, new ObjectMapper(), properties);
  }

  @Test
  void parsesGnewsResponse() {
    String payload = """
        {
          "totalArticles": 1,
          "articles": [
            {
              "id": "abc123",
              "title": "Sample",
              "description": "Desc",
              "content": "Content",
              "url": "https://example.com/article",
              "image": "https://example.com/image.jpg",
              "publishedAt": "2024-01-01T12:00:00Z",
              "lang": "en",
              "source": {
                "id": "source-id",
                "name": "Source Name",
                "url": "https://source.com",
                "country": "US"
              }
            }
          ]
        }
        """;
    server.expect(requestTo("https://gnews.test/api/v4/search?q=apple&lang=en"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

    FetchRequest request = new FetchRequest();
    request.setEndpointType(EndpointType.SEARCH);
    request.setQuery("apple");
    request.setLang("en");

    var result = provider.fetch(request, "key");
    assertThat(result.getTotalArticles()).isEqualTo(1);
    assertThat(result.getArticles()).hasSize(1);
    assertThat(result.getArticles().get(0).getTitle()).isEqualTo("Sample");
  }

  @Test
  void mapsRetryableErrors() {
    server.expect(requestTo("https://gnews.test/api/v4/search?q=apple&lang=en"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

    FetchRequest request = new FetchRequest();
    request.setEndpointType(EndpointType.SEARCH);
    request.setQuery("apple");
    request.setLang("en");

    assertThatThrownBy(() -> provider.fetch(request, "key"))
        .isInstanceOf(ProviderException.class)
        .satisfies(ex -> assertThat(((ProviderException) ex).isRetryable()).isTrue());
  }
}
