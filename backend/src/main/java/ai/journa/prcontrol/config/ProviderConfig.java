package ai.journa.prcontrol.config;

import ai.journa.prcontrol.service.integration.GNewsProvider;
import ai.journa.prcontrol.service.integration.MockNewsProvider;
import ai.journa.prcontrol.service.integration.NewsProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@EnableConfigurationProperties(NewsProviderProperties.class)
public class ProviderConfig {
  @Bean
  public RestTemplate restTemplate(NewsProviderProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    int timeoutMs = properties.getGnews().getRequestTimeoutSeconds() * 1000;
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    return new RestTemplate(factory);
  }

  @Bean
  public NewsProvider gNewsProvider(RestTemplate restTemplate,
                                    ObjectMapper objectMapper,
                                    NewsProviderProperties properties) {
    return new GNewsProvider(restTemplate, objectMapper, properties);
  }

  @Bean
  public NewsProvider mockNewsProvider(ObjectMapper objectMapper) {
    return new MockNewsProvider(objectMapper);
  }

  @Bean
  public List<NewsProvider> newsProviders(NewsProvider gNewsProvider, NewsProvider mockNewsProvider) {
    return List.of(gNewsProvider, mockNewsProvider);
  }
}
