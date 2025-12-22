package ai.journa.prcontrol.config;

import ai.journa.prcontrol.service.integration.EmailProvider;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.integration.real.GNewsProvider;
import ai.journa.prcontrol.service.integration.real.RssNewsProvider;
import ai.journa.prcontrol.service.mock.MockEmailProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProviderConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public NewsProvider gNewsProvider(RestTemplate restTemplate, ObjectMapper objectMapper, NewsProviderProperties properties) {
        return new GNewsProvider(restTemplate, objectMapper, properties.getGnews().getApiKey());
    }

    @Bean
    public NewsProvider rssNewsProvider(ObjectMapper objectMapper, NewsProviderProperties properties) {
        return new RssNewsProvider(objectMapper, properties.getRss().getFeeds());
    }

    @Bean
    public EmailProvider emailProvider() {
        return new MockEmailProvider();
    }
}
