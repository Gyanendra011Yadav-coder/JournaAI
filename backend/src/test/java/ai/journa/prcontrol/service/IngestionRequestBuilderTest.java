package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryTemplate;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.repository.BeatQueryTemplateRepository;
import ai.journa.prcontrol.repository.UserClientRepository;
import ai.journa.prcontrol.repository.UserKeywordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngestionRequestBuilderTest {
  @Test
  void buildsQueriesFromTemplate() {
    Beat beat = new Beat();
    beat.setId(1L);
    beat.setName("Finance");

    BeatQueryTemplate template = new BeatQueryTemplate();
    template.setBeat(beat);
    template.setEndpointType(EndpointType.SEARCH);
    template.setBeatTerms(List.of("Finance"));
    template.setLangDefault("en");
    template.setCountryDefault("us");

    BeatQueryTemplateRepository templateRepository = mock(BeatQueryTemplateRepository.class);
    when(templateRepository.findByBeatId(1L)).thenReturn(List.of(template));

    QueryBuilderService queryBuilderService = new QueryBuilderService(
        templateRepository,
        mock(UserKeywordRepository.class),
        mock(UserClientRepository.class)
    );

    QueryBuilderService.QueryBundle bundle = queryBuilderService.buildSearchQueries(beat, null);
    assertThat(bundle.beatQuery()).isEqualTo("Finance");
    assertThat(bundle.clientQuery()).isEqualTo("Finance");
    assertThat(bundle.template()).isEqualTo(template);
  }
}
