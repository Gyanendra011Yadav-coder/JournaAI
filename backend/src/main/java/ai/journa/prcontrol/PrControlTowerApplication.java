package ai.journa.prcontrol;

import ai.journa.prcontrol.config.BeatProperties;
import ai.journa.prcontrol.config.CorsProperties;
import ai.journa.prcontrol.config.EnrichmentProperties;
import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.config.RequestLoggingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    NewsProviderProperties.class,
    BeatProperties.class,
    CorsProperties.class,
    EnrichmentProperties.class,
    RequestLoggingProperties.class
})
public class PrControlTowerApplication {
  public static void main(String[] args) {
    SpringApplication.run(PrControlTowerApplication.class, args);
  }
}
