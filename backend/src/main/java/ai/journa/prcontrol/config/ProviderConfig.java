package ai.journa.prcontrol.config;

import ai.journa.prcontrol.service.integration.EmailProvider;
import ai.journa.prcontrol.service.integration.MediaDatabaseProvider;
import ai.journa.prcontrol.service.integration.NewsProvider;
import ai.journa.prcontrol.service.mock.MockEmailProvider;
import ai.journa.prcontrol.service.mock.MockMediaDatabaseProvider;
import ai.journa.prcontrol.service.mock.MockNewsProvider;
import ai.journa.prcontrol.service.integration.real.NewsApiProvider;
import ai.journa.prcontrol.service.integration.real.SmtpEmailProvider;
import ai.journa.prcontrol.service.integration.real.MediaDatabaseApiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {
    @Bean
    public NewsProvider newsProvider(@Value("${app.providers.news:mock}") String mode) {
        if ("real".equalsIgnoreCase(mode)) {
            return new NewsApiProvider();
        }
        return new MockNewsProvider();
    }

    @Bean
    public MediaDatabaseProvider mediaDatabaseProvider(@Value("${app.providers.media:mock}") String mode) {
        if ("real".equalsIgnoreCase(mode)) {
            return new MediaDatabaseApiProvider();
        }
        return new MockMediaDatabaseProvider();
    }

    @Bean
    public EmailProvider emailProvider(@Value("${app.providers.email:mock}") String mode) {
        if ("real".equalsIgnoreCase(mode)) {
            return new SmtpEmailProvider();
        }
        return new MockEmailProvider();
    }
}
