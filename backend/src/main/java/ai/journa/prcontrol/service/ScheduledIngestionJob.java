package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.IngestMode;
import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.repository.BeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledIngestionJob {
  private final BeatRepository beatRepository;
  private final IntegrationSettingsService integrationSettingsService;
  private final IngestionService ingestionService;
  private final LocaleResolver localeResolver;

  public ScheduledIngestionJob(BeatRepository beatRepository,
                               IntegrationSettingsService integrationSettingsService,
                               IngestionService ingestionService,
                               LocaleResolver localeResolver) {
    this.beatRepository = beatRepository;
    this.integrationSettingsService = integrationSettingsService;
    this.ingestionService = ingestionService;
    this.localeResolver = localeResolver;
  }

  @Scheduled(fixedDelayString = "60000")
  public void refreshBeats() {
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    if (!settings.isEnabled()) {
      return;
    }
    LocaleResolver.Resolution locale = localeResolver.resolveDefaults();
    List<Beat> beats = beatRepository.findAll().stream().filter(Beat::isActive).toList();
    for (Beat beat : beats) {
      IngestionService.IngestRequest request = new IngestionService.IngestRequest(
          IngestMode.SEARCH, beat.getId(), null, "BEAT", null, null);
      ingestionService.refresh(request, locale, null, true);
    }
    IngestionService.IngestRequest localTrending = new IngestionService.IngestRequest(
        IngestMode.TRENDING, null, "general", "LOCAL", null, null);
    ingestionService.refresh(localTrending, locale, null, true);
    IngestionService.IngestRequest globalTrending = new IngestionService.IngestRequest(
        IngestMode.TRENDING, null, "general", "GLOBAL", null, null);
    ingestionService.refresh(globalTrending, locale, null, true);
  }
}
