package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryRecipe;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.BeatQueryRecipeRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import ai.journa.prcontrol.service.integration.NewsProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IngestionRequestBuilderTest {
  @Test
  void buildsRequestWithDefaults() {
    Beat beat = new Beat();
    beat.setId(1L);

    BeatQueryRecipe recipe = new BeatQueryRecipe();
    recipe.setBeat(beat);
    recipe.setEndpointType(EndpointType.SEARCH);
    recipe.setQuery("Finance");
    recipe.setLang(null);

    IntegrationSettings settings = new IntegrationSettings();
    settings.setProviderType(ProviderType.GNEWS);
    settings.setDefaultLang("en");
    settings.setDefaultCountry("us");
    settings.setMaxPerRequest(25);

    IngestionService ingestionService = new IngestionService(
        mock(BeatRepository.class),
        mock(BeatQueryRecipeRepository.class),
        mock(IntegrationSettingsService.class),
        mock(NewsFetchStateRepository.class),
        mock(ArticleRepository.class),
        mock(AuditService.class),
        mock(OutboundRateLimiter.class),
        new NewsProviderProperties(),
        List.of(mock(NewsProvider.class))
    );

    var request = ingestionService.buildFetchRequest(recipe, settings);
    assertThat(request.getQuery()).isEqualTo("Finance");
    assertThat(request.getLang()).isEqualTo("en");
    assertThat(request.getCountry()).isEqualTo("us");
    assertThat(request.getMax()).isEqualTo(25);
  }
}
