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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final TaskExecutor taskExecutor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public EnrichmentTaskRunner(EnrichmentTaskRepository taskRepository,
                              ArticleRepository articleRepository,
                              ArticleAuthorExtractionRepository extractionRepository,
                              AuthorExtractionService authorExtractionService,
                              AuthorNormalizationService normalizationService,
                              JournalistResolutionService resolutionService,
                              ObjectMapper objectMapper,
                              EnrichmentProperties properties,
                              @Qualifier("enrichmentTaskExecutor") TaskExecutor taskExecutor) {
    this.taskRepository = taskRepository;
    this.articleRepository = articleRepository;
    this.extractionRepository = extractionRepository;
    this.authorExtractionService = authorExtractionService;
    this.normalizationService = normalizationService;
    this.resolutionService = resolutionService;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.taskExecutor = taskExecutor;
  }

  @Scheduled(fixedDelayString = "${app.enrichment.scheduled.fixedDelayMs:600000}")
  public void runScheduled() {
    if (properties.getScheduled().isEnabled()) {
      runPendingTasks();
    }
  }

  public void runPendingTasks() {
    if (!running.compareAndSet(false, true)) {
      logger.info("Enrichment run: already running");
      return;
    }
    try {
      Instant now = Instant.now();
      EnrichmentProperties.Runner runner = properties.getRunner();
      int batchSize = Math.max(1, runner.getBatchSize());
      int reviewBatchSize = Math.max(0, Math.min(runner.getReviewBatchSize(), batchSize));
      int oldPendingBatchSize = Math.max(0, Math.min(runner.getOldPendingBatchSize(), batchSize - reviewBatchSize));
      int newPendingBatchSize = Math.max(0, batchSize - reviewBatchSize - oldPendingBatchSize);

      Map<Long, EnrichmentTask> selected = new LinkedHashMap<>();
      if (newPendingBatchSize > 0) {
        List<EnrichmentTask> newestPending = taskRepository
            .findByStatusAndNextRunAtBeforeOrderByPriorityDescCreatedAtDesc(
                EnrichmentTaskStatus.PENDING,
                now,
                PageRequest.of(0, newPendingBatchSize)
            );
        newestPending.forEach(task -> selected.put(task.getId(), task));
      }

      if (oldPendingBatchSize > 0) {
        List<EnrichmentTask> oldestPending = taskRepository
            .findByStatusAndNextRunAtBeforeOrderByPriorityDescCreatedAtAsc(
                EnrichmentTaskStatus.PENDING,
                now,
                PageRequest.of(0, oldPendingBatchSize)
            );
        oldestPending.forEach(task -> selected.putIfAbsent(task.getId(), task));
      }

      int remaining = batchSize - selected.size();
      if (reviewBatchSize > 0 && remaining > 0) {
        int reviewLimit = Math.min(reviewBatchSize, remaining);
        List<EnrichmentTask> reviewTasks = taskRepository
            .findByStatusAndNextRunAtBeforeOrderByPriorityDescCreatedAtAsc(
                EnrichmentTaskStatus.NEEDS_REVIEW,
                now,
                PageRequest.of(0, reviewLimit)
            );
        reviewTasks.forEach(task -> selected.putIfAbsent(task.getId(), task));
      }

      List<EnrichmentTask> tasks = new ArrayList<>(selected.values());
      if (tasks.isEmpty()) {
        logger.info("Enrichment run: no pending tasks");
        return;
      }
      logger.info("Enrichment run: processing {} task(s) [pendingNewest={}, pendingOldest={}, review={}]",
          tasks.size(),
          newPendingBatchSize,
          oldPendingBatchSize,
          reviewBatchSize);
      executeTasks(tasks);
    } finally {
      running.set(false);
    }
  }

  public void runTasks(List<EnrichmentTask> tasks) {
    if (tasks == null || tasks.isEmpty()) {
      logger.info("Enrichment run: no tasks to process");
      return;
    }
    if (running.compareAndSet(false, true)) {
      try {
        logger.info("Enrichment run: processing {} task(s) [auto]", tasks.size());
        executeTasks(tasks);
      } finally {
        running.set(false);
      }
      return;
    }
    logger.info("Enrichment run: already running, queueing {} task(s) [auto]", tasks.size());
    executeTasks(tasks);
  }

  private void executeTasks(List<EnrichmentTask> tasks) {
    Executor executor = taskExecutor::execute;
    List<CompletableFuture<Void>> futures = tasks.stream()
        .map(task -> CompletableFuture.runAsync(() -> {
          try {
            runTask(task);
          } catch (Exception ex) {
            logger.warn("Enrichment task execution failed taskId={} reason={}",
                task.getId(), ex.getMessage());
          }
        }, executor))
        .toList();
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
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
    Long taskId = task.getId();
    EnrichmentTask current = taskId != null ? taskRepository.findById(taskId).orElse(null) : null;
    if (current == null) {
      logger.warn("Enrichment task skipped reason=NOT_FOUND taskId={}", taskId);
      return;
    }
    if (current.getStatus() != EnrichmentTaskStatus.PENDING
        && current.getStatus() != EnrichmentTaskStatus.NEEDS_REVIEW) {
      logger.info("Enrichment task skipped reason=ALREADY_PROCESSED taskId={} status={}",
          taskId, current.getStatus());
      return;
    }
    current.setStatus(EnrichmentTaskStatus.RUNNING);
    current.setAttempts(current.getAttempts() + 1);
    current.setUpdatedAt(Instant.now());
    try {
      current = taskRepository.save(current);
    } catch (ObjectOptimisticLockingFailureException ex) {
      logger.info("Enrichment task skipped reason=CONCURRENT_UPDATE taskId={}", taskId);
      return;
    }
    logger.info("Enrichment task start type={} articleId={} attempt={}",
        current.getTaskType(),
        current.getArticle() != null ? current.getArticle().getId() : null,
        current.getAttempts());
    try {
      if (current.getTaskType() == EnrichmentTaskType.EXTRACT_AUTHOR) {
        handleExtractAuthor(current);
      } else if (current.getTaskType() == EnrichmentTaskType.RESOLVE_JOURNALIST) {
        handleResolveJournalist(current);
      } else {
        current.setStatus(EnrichmentTaskStatus.SKIPPED);
        current.setNotes("No enrichment handler configured");
        current.setUpdatedAt(Instant.now());
        taskRepository.save(current);
      }
    } catch (Exception ex) {
      current.setStatus(EnrichmentTaskStatus.FAILED);
      current.setNotes(ex.getMessage());
      current.setUpdatedAt(Instant.now());
      taskRepository.save(current);
      logger.warn("Enrichment task failed type={} articleId={} reason={}",
          current.getTaskType(),
          current.getArticle() != null ? current.getArticle().getId() : null,
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
      AuthorExtractionService.Candidate bestCandidate = result.candidates().stream()
          .max(Comparator.comparingInt(AuthorExtractionService.Candidate::confidence))
          .orElse(null);
      String resolvedName = normalized.stream()
          .filter(candidate -> !candidate.nonPerson())
          .map(AuthorNormalizationService.Candidate::name)
          .findFirst()
          .orElse(bestCandidate != null ? bestCandidate.name() : result.authorRaw());
      int confidence = bestCandidate != null ? bestCandidate.confidence() : 70;

      if (properties.isAutoRunAfterIngest() && resolvedName != null && !resolvedName.isBlank()) {
        resolutionService.resolve(article, resolvedName, confidence);
        task.setNotes("Extraction stored id=" + extraction.getId() + "; inline resolve");
        task.setStatus(EnrichmentTaskStatus.DONE);
      } else {
        EnrichmentTask resolveTask = new EnrichmentTask();
        resolveTask.setTaskType(EnrichmentTaskType.RESOLVE_JOURNALIST);
        resolveTask.setArticle(article);
        resolveTask.setStatus(EnrichmentTaskStatus.PENDING);
        resolveTask.setNextRunAt(Instant.now());
        taskRepository.save(resolveTask);
        task.setNotes("Extraction stored id=" + extraction.getId());
        task.setStatus(EnrichmentTaskStatus.DONE);
      }
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
