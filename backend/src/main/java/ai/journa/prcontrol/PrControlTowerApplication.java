package ai.journa.prcontrol;

import ai.journa.prcontrol.config.NewsProviderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NewsProviderProperties.class)
public class PrControlTowerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrControlTowerApplication.class, args);
    }
}
