package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.EnrichmentProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.EnrichmentTask;
import ai.journa.prcontrol.domain.EnrichmentTaskStatus;
import ai.journa.prcontrol.domain.EnrichmentTaskType;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.EnrichmentTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EnrichmentTaskRunner {
  private static final Logger logger = LoggerFactory.getLogger(EnrichmentTaskRunner.class);
  private final EnrichmentTaskRepository taskRepository;
  private final ArticleRepository articleRepository;
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final AuthorExtractionService authorExtractionService;
  private final AuthorNormalizationService normalizationService;
  private final JournalistResolutionService resolutionService;
  private final ObjectMapper objectMapper;
  private final EnrichmentProperties properties;

  public EnrichmentTaskRunner(EnrichmentTaskRepository taskRepository,
                              ArticleRepository articleRepository,
                              ArticleAuthorExtractionRepository extractionRepository,
                              AuthorExtractionService authorExtractionService,
                              AuthorNormalizationService normalizationService,
                              JournalistResolutionService resolutionService,
                              ObjectMapper objectMapper,
                              EnrichmentProperties properties) {
    this.taskRepository = taskRepository;
    this.articleRepository = articleRepository;
    this.extractionRepository = extractionRepository;
    this.authorExtractionService = authorExtractionService;
    this.normalizationService = normalizationService;
    this.resolutionService = resolutionService;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${app.enrichment.scheduled.fixedDelayMs:600000}")
  public void runScheduled() {
    if (properties.getScheduled().isEnabled()) {
      runPendingTasks();
    }
  }

  public void runPendingTasks() {
    List<EnrichmentTask> tasks = taskRepository.findTop50ByStatusInAndNextRunAtBeforeOrderByPriorityDescCreatedAtAsc(
        List.of(EnrichmentTaskStatus.PENDING, EnrichmentTaskStatus.NEEDS_REVIEW),
        Instant.now()
    );
    if (tasks.isEmpty()) {
      logger.info("Enrichment run: no pending tasks");
    } else {
      logger.info("Enrichment run: processing {} task(s)", tasks.size());
    }
    tasks.forEach(this::runTask);
  }

  public EnrichmentTask enqueueTask(EnrichmentTaskType type, Article article) {
    EnrichmentTask task = new EnrichmentTask();
    task.setTaskType(type);
    task.setArticle(article);
    task.setStatus(EnrichmentTaskStatus.PENDING);
    task.setNextRunAt(Instant.now());
    return taskRepository.save(task);
  }

  public EnrichmentTask enqueueTask(EnrichmentTaskType type, Article article, String notes) {
    EnrichmentTask task = enqueueTask(type, article);
    task.setNotes(notes);
    return taskRepository.save(task);
  }

  public EnrichmentTask runOnDemand(Long articleId, Long journalistId) {
    EnrichmentTask task = new EnrichmentTask();
    task.setTaskType(articleId != null ? EnrichmentTaskType.EXTRACT_AUTHOR : EnrichmentTaskType.ENRICH_PROFILE);
    if (articleId != null) {
      Article article = articleRepository.findById(articleId)
          .orElseThrow(() -> new IllegalStateException("Article not found"));
      task.setArticle(article);
    }
    if (journalistId != null) {
      task.setNotes("Manual enrichment for journalist " + journalistId);
    }
    task.setStatus(EnrichmentTaskStatus.PENDING);
    task.setNextRunAt(Instant.now());
    return taskRepository.save(task);
  }

  private void runTask(EnrichmentTask task) {
    task.setStatus(EnrichmentTaskStatus.RUNNING);
    task.setAttempts(task.getAttempts() + 1);
    task.setUpdatedAt(Instant.now());
    taskRepository.save(task);
    logger.info("Enrichment task start type={} articleId={} attempt={}",
        task.getTaskType(),
        task.getArticle() != null ? task.getArticle().getId() : null,
        task.getAttempts());
    try {
      if (task.getTaskType() == EnrichmentTaskType.EXTRACT_AUTHOR) {
        handleExtractAuthor(task);
      } else if (task.getTaskType() == EnrichmentTaskType.RESOLVE_JOURNALIST) {
        handleResolveJournalist(task);
      } else {
        task.setStatus(EnrichmentTaskStatus.SKIPPED);
        task.setNotes("No enrichment handler configured");
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
      }
    } catch (Exception ex) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes(ex.getMessage());
      task.setUpdatedAt(Instant.now());
      taskRepository.save(task);
      logger.warn("Enrichment task failed type={} articleId={} reason={}",
          task.getTaskType(),
          task.getArticle() != null ? task.getArticle().getId() : null,
          ex.getMessage());
    }
  }

  private void handleExtractAuthor(EnrichmentTask task) throws JsonProcessingException {
    Article article = task.getArticle();
    if (article == null) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes("Missing article");
      taskRepository.save(task);
      return;
    }
    AuthorExtractionService.ExtractionResult result = authorExtractionService.extractForArticle(article);
    List<String> candidateNames = result.candidates().stream().map(AuthorExtractionService.Candidate::name).toList();
    List<AuthorNormalizationService.Candidate> normalized = normalizationService.normalizeCandidates(candidateNames);
    boolean nonPerson = normalized.stream().allMatch(AuthorNormalizationService.Candidate::nonPerson);

    String candidatesJsonb = objectMapper.writeValueAsString(result.candidates());
    ArticleAuthorExtraction extraction = authorExtractionService.persistExtraction(article, result, candidatesJsonb, nonPerson);
    logger.info("Author extraction result articleId={} status={} candidates={} nonPerson={}",
        article.getId(),
        result.fetchStatus(),
        result.candidates().size(),
        nonPerson);

    if (result.fetchStatus() == FetchStatus.SUCCESS && !nonPerson && !normalized.isEmpty()) {
      EnrichmentTask resolveTask = new EnrichmentTask();
      resolveTask.setTaskType(EnrichmentTaskType.RESOLVE_JOURNALIST);
      resolveTask.setArticle(article);
      resolveTask.setStatus(EnrichmentTaskStatus.PENDING);
      resolveTask.setNextRunAt(Instant.now());
      taskRepository.save(resolveTask);
      task.setNotes("Extraction stored id=" + extraction.getId());
      task.setStatus(EnrichmentTaskStatus.DONE);
    } else if (result.fetchStatus() == FetchStatus.SKIPPED) {
      task.setNotes(result.errorMessage() != null ? result.errorMessage() : "Extraction skipped");
      task.setStatus(EnrichmentTaskStatus.SKIPPED);
    } else if (nonPerson) {
      task.setNotes("Non-person byline detected");
      task.setStatus(EnrichmentTaskStatus.DONE);
    } else if (result.fetchStatus() != FetchStatus.SUCCESS) {
      task.setNotes("Extraction failed: " + result.errorCode());
      task.setStatus(EnrichmentTaskStatus.FAILED);
    } else {
      task.setNotes("No author candidates found");
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
    }
    task.setUpdatedAt(Instant.now());
    taskRepository.save(task);
    logger.info("Author extraction done articleId={} taskStatus={} notes={}",
        article.getId(),
        task.getStatus(),
        task.getNotes());
  }

  private void handleResolveJournalist(EnrichmentTask task) {
    Article article = task.getArticle();
    if (article == null) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes("Missing article");
      taskRepository.save(task);
      return;
    }
    Optional<ArticleAuthorExtraction> extraction = extractionRepository.findFirstByArticleIdOrderByExtractedAtDesc(article.getId());
    if (extraction.isEmpty() || extraction.get().getAuthorRaw() == null) {
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
      task.setNotes("Missing author extraction");
      taskRepository.save(task);
      logger.info("Journalist resolve skipped articleId={} reason=NO_EXTRACTION",
          article.getId());
      return;
    }
    String authorName = extraction.get().getAuthorRaw();
    int confidence = 70;
    resolutionService.resolve(article, authorName, confidence);
    task.setStatus(confidence < 80 ? EnrichmentTaskStatus.NEEDS_REVIEW : EnrichmentTaskStatus.DONE);
    task.setNotes(confidence < 80 ? "Fuzzy match" : "Resolved");
    task.setUpdatedAt(Instant.now());
    taskRepository.save(task);
    logger.info("Journalist resolve done articleId={} author={} status={}",
        article.getId(),
        authorName,
        task.getStatus());
  }
}
