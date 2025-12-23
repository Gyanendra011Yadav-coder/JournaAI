package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryRecipe;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatQueryRecipeRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.IntegrationSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
class IngestionIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
      .withDatabaseName("prcontrol")
      .withUsername("prcontrol")
      .withPassword("prcontrol");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.security.integrationMasterKey", () -> "test-master-key");
  }

  @Autowired
  private BeatRepository beatRepository;
  @Autowired
  private BeatQueryRecipeRepository recipeRepository;
  @Autowired
  private IntegrationSettingsRepository integrationSettingsRepository;
  @Autowired
  private IngestionService ingestionService;
  @Autowired
  private ArticleRepository articleRepository;
  @Autowired
  private EncryptionService encryptionService;

  @Test
  void ingestsArticlesIntoDatabase() {
    Beat beat = new Beat();
    beat.setName("Test Beat");
    beat.setSlug("test-beat");
    beat.setActive(true);
    beat = beatRepository.save(beat);

    BeatQueryRecipe recipe = new BeatQueryRecipe();
    recipe.setBeat(beat);
    recipe.setEndpointType(EndpointType.SEARCH);
    recipe.setQuery("Testing");
    recipe.setLang("en");
    recipe.setCountry("us");
    recipe.setMax(5);
    recipeRepository.save(recipe);

    IntegrationSettings settings = integrationSettingsRepository.findAll().stream().findFirst().orElse(null);
    if (settings == null) {
      settings = new IntegrationSettings();
      settings.setProviderType(ProviderType.MOCK);
    }
    settings.setProviderType(ProviderType.MOCK);
    settings.setEnabled(true);
    settings.setApiKeyEncrypted(encryptionService.encrypt("mock"));
    settings.setDefaultLang("en");
    settings.setDefaultCountry("us");
    settings.setMaxPerRequest(5);
    integrationSettingsRepository.save(settings);

    var result = ingestionService.refreshBeat(beat.getId(), null, false);
    assertThat(result.isSuccess()).isTrue();
    assertThat(articleRepository.findAll()).isNotEmpty();
  }
}
