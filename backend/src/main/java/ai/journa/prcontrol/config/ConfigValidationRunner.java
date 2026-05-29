package ai.journa.prcontrol.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ConfigValidationRunner implements ApplicationRunner {
  private final String integrationMasterKey;
  private final String jwtSecret;

  public ConfigValidationRunner(@Value("${app.security.integrationMasterKey}") String integrationMasterKey,
                                @Value("${app.jwt.secret}") String jwtSecret) {
    this.integrationMasterKey = integrationMasterKey;
    this.jwtSecret = jwtSecret;
  }

  @Override
  public void run(ApplicationArguments args) {
    validateMasterKey();
    validateJwtSecret();
  }

  private void validateMasterKey() {
    if (integrationMasterKey == null || integrationMasterKey.isBlank()) {
      throw new IllegalStateException("Missing app.security.integrationMasterKey; provide a strong secret.");
    }
    if (isPlaceholderSecret(integrationMasterKey)) {
      throw new IllegalStateException("Replace app.security.integrationMasterKey with a strong secret.");
    }
    if (integrationMasterKey.length() < 32) {
      throw new IllegalStateException("app.security.integrationMasterKey must be at least 32 characters.");
    }
  }

  private void validateJwtSecret() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("Missing app.jwt.secret; provide a strong secret.");
    }
    if (isPlaceholderSecret(jwtSecret)) {
      throw new IllegalStateException("Replace app.jwt.secret with a strong secret.");
    }
    if (jwtSecret.length() < 32) {
      throw new IllegalStateException("app.jwt.secret must be at least 32 characters.");
    }
  }

  private boolean isPlaceholderSecret(String value) {
    return value != null && value.toLowerCase().startsWith("change-me");
  }

}
