package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.ProviderType;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ScheduledIngestionJob {
  private final BeatRepository beatRepository;
  private final NewsFetchStateRepository newsFetchStateRepository;
  private final IntegrationSettingsService integrationSettingsService;
  private final IngestionService ingestionService;

  public ScheduledIngestionJob(BeatRepository beatRepository,
                               NewsFetchStateRepository newsFetchStateRepository,
                               IntegrationSettingsService integrationSettingsService,
                               IngestionService ingestionService) {
    this.beatRepository = beatRepository;
    this.newsFetchStateRepository = newsFetchStateRepository;
    this.integrationSettingsService = integrationSettingsService;
    this.ingestionService = ingestionService;
  }

  @Scheduled(fixedDelayString = "60000")
  public void refreshBeats() {
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    if (!settings.isEnabled()) {
      return;
    }
    int refreshInterval = settings.getRefreshIntervalMinutes() != null ? settings.getRefreshIntervalMinutes() : 30;
    Instant now = Instant.now();
    List<Beat> beats = beatRepository.findAll().stream().filter(Beat::isActive).toList();
    for (Beat beat : beats) {
      newsFetchStateRepository.findByBeatId(beat.getId()).ifPresentOrElse(state -> {
        if (state.getLastAttemptAt() == null ||
            state.getLastAttemptAt().isBefore(now.minus(Duration.ofMinutes(refreshInterval)))) {
          ingestionService.refreshBeat(beat.getId(), null, true);
        }
      }, () -> ingestionService.refreshBeat(beat.getId(), null, true));
    }
  }
}
