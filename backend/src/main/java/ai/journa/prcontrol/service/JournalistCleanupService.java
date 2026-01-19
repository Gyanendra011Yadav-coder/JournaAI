package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.JournalistCleanupProperties;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistVerificationStatus;
import ai.journa.prcontrol.repository.JournalistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JournalistCleanupService {
  private static final Logger logger = LoggerFactory.getLogger(JournalistCleanupService.class);
  private final JournalistRepository journalistRepository;
  private final AuthorNormalizationService normalizationService;
  private final JournalistCleanupProperties properties;

  public JournalistCleanupService(JournalistRepository journalistRepository,
                                  AuthorNormalizationService normalizationService,
                                  JournalistCleanupProperties properties) {
    this.journalistRepository = journalistRepository;
    this.normalizationService = normalizationService;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${app.journalist-cleanup.fixedDelayMs:600000}")
  public void removeNonPersonJournalists() {
    if (!properties.isEnabled()) {
      return;
    }
    int batchSize = Math.max(1, properties.getBatchSize());
    long lastId = 0L;
    int deleted = 0;
    while (true) {
      List<Journalist> batch = journalistRepository
          .findByIdGreaterThanOrderByIdAsc(lastId, PageRequest.of(0, batchSize));
      if (batch.isEmpty()) {
        break;
      }
      for (Journalist journalist : batch) {
        lastId = journalist.getId();
        if (journalist.getVerificationStatus() == JournalistVerificationStatus.VERIFIED) {
          continue;
        }
        String name = journalist.getFullName();
        if (normalizationService.isNonPerson(name) && !normalizationService.looksLikePersonName(name)) {
          journalistRepository.delete(journalist);
          deleted++;
          logger.info("Journalist cleanup: removed non-person journalistId={} name={}", journalist.getId(), name);
        }
      }
    }
    if (deleted > 0) {
      logger.info("Journalist cleanup: removed {} non-person journalists", deleted);
    }
  }
}
