package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.EnrichmentProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.ArticleAttribution;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.AttributionType;
import ai.journa.prcontrol.domain.AuthorClassification;
import ai.journa.prcontrol.domain.AuthorExtractionStatus;
import ai.journa.prcontrol.domain.EnrichmentTask;
import ai.journa.prcontrol.domain.EnrichmentTaskStatus;
import ai.journa.prcontrol.domain.EnrichmentTaskType;
import ai.journa.prcontrol.domain.FetchStatus;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.repository.ArticleAttributionRepository;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.EnrichmentTaskRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EnrichmentTaskRunner {
  private static final Logger logger = LoggerFactory.getLogger(EnrichmentTaskRunner.class);
  private final EnrichmentTaskRepository taskRepository;
  private final ArticleRepository articleRepository;
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ArticleAttributionRepository attributionRepository;
  private final ArticleJournalistRepository articleJournalistRepository;
  private final JournalistRepository journalistRepository;
  private final AuthorExtractionService authorExtractionService;
  private final AuthorNormalizationService normalizationService;
  private final JournalistResolutionService resolutionService;
  private final JournalistEnrichmentService journalistEnrichmentService;
  private final ObjectMapper objectMapper;
  private final EnrichmentProperties properties;
  private final TaskExecutor taskExecutor;
  private final TransactionTemplate transactionTemplate;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public EnrichmentTaskRunner(EnrichmentTaskRepository taskRepository,
                              ArticleRepository articleRepository,
                              ArticleAuthorExtractionRepository extractionRepository,
                              ArticleAttributionRepository attributionRepository,
                              ArticleJournalistRepository articleJournalistRepository,
                              JournalistRepository journalistRepository,
                              AuthorExtractionService authorExtractionService,
                              AuthorNormalizationService normalizationService,
                              JournalistResolutionService resolutionService,
                              JournalistEnrichmentService journalistEnrichmentService,
                              ObjectMapper objectMapper,
                              EnrichmentProperties properties,
                              @Qualifier("enrichmentTaskExecutor") TaskExecutor taskExecutor,
                              TransactionTemplate transactionTemplate) {
    this.taskRepository = taskRepository;
    this.articleRepository = articleRepository;
    this.extractionRepository = extractionRepository;
    this.attributionRepository = attributionRepository;
    this.articleJournalistRepository = articleJournalistRepository;
    this.journalistRepository = journalistRepository;
    this.authorExtractionService = authorExtractionService;
    this.normalizationService = normalizationService;
    this.resolutionService = resolutionService;
    this.journalistEnrichmentService = journalistEnrichmentService;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.taskExecutor = taskExecutor;
    this.transactionTemplate = transactionTemplate;
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
    List<Long> taskIds = tasks.stream()
        .map(EnrichmentTask::getId)
        .filter(Objects::nonNull)
        .toList();
    List<CompletableFuture<Void>> futures = taskIds.stream()
        .map(taskId -> CompletableFuture.runAsync(() -> runSingleTask(taskId), executor))
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
    if (articleId == null && journalistId == null) {
      throw new IllegalArgumentException("Article or journalist id is required");
    }
    List<EnrichmentTaskStatus> eligible = List.of(
        EnrichmentTaskStatus.PENDING,
        EnrichmentTaskStatus.NEEDS_REVIEW,
        EnrichmentTaskStatus.FAILED
    );
    Optional<EnrichmentTask> existing = articleId != null
        ? taskRepository.findTopByArticleIdAndStatusInOrderByCreatedAtDesc(articleId, eligible)
        : taskRepository.findTopByJournalistIdAndStatusInOrderByCreatedAtDesc(journalistId, eligible);
    EnrichmentTask task = existing.orElseGet(() -> {
      EnrichmentTask fresh = new EnrichmentTask();
      fresh.setTaskType(articleId != null ? EnrichmentTaskType.EXTRACT_AUTHOR : EnrichmentTaskType.ENRICH_PROFILE);
      if (articleId != null) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalStateException("Article not found"));
        fresh.setArticle(article);
      }
      if (journalistId != null) {
        Journalist journalist = journalistRepository.findById(journalistId)
            .orElseThrow(() -> new IllegalStateException("Journalist not found"));
        fresh.setJournalist(journalist);
        fresh.setNotes("Manual enrichment for journalist " + journalistId);
      }
      fresh.setStatus(EnrichmentTaskStatus.PENDING);
      fresh.setNextRunAt(Instant.now());
      return fresh;
    });
    task.setStatus(EnrichmentTaskStatus.PENDING);
    task.setNextRunAt(Instant.now());
    EnrichmentTask saved = taskRepository.save(task);
    runSingleTask(saved.getId());
    return taskRepository.findWithArticleAndJournalistById(saved.getId()).orElse(saved);
  }

  private void runSingleTask(Long taskId) {
    if (taskId == null) {
      return;
    }
    transactionTemplate.executeWithoutResult(status -> {
      try {
        runTaskInternal(taskId);
      } catch (Exception ex) {
        logger.warn("Enrichment task execution failed taskId={} reason={}", taskId, ex.getMessage());
      }
    });
  }

  private void runTaskInternal(Long taskId) {
    EnrichmentTask current = taskRepository.findWithArticleAndJournalistById(taskId).orElse(null);
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
    Long articleId = current.getArticle() != null ? current.getArticle().getId() : null;
    logger.info("Enrichment task start type={} articleId={} attempt={}",
        current.getTaskType(),
        articleId,
        current.getAttempts());
    try {
      if (current.getTaskType() == EnrichmentTaskType.EXTRACT_AUTHOR) {
        handleExtractAuthor(current);
      } else if (current.getTaskType() == EnrichmentTaskType.RESOLVE_JOURNALIST) {
        handleResolveJournalist(current);
      } else if (current.getTaskType() == EnrichmentTaskType.ENRICH_PROFILE) {
        handleEnrichProfile(current);
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
          articleId,
          ex.getMessage());
    }
  }

  private void handleExtractAuthor(EnrichmentTask task) throws JsonProcessingException {
    Article article = loadArticle(task);
    if (article == null) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes("Missing article");
      safeSaveTask(task, "missing article");
      return;
    }
    AuthorExtractionService.ExtractionResult result = authorExtractionService.extractForArticle(article);
    String candidatesJsonb = objectMapper.writeValueAsString(result.candidates());
    ArticleAuthorExtraction extraction = authorExtractionService.persistExtraction(article, result, candidatesJsonb);
    logger.info("Author extraction result articleId={} status={} candidates={} classification={}",
        article.getId(),
        result.fetchStatus(),
        result.candidates().size(),
        result.classification());

    if (result.classification() == AuthorClassification.BUREAU && result.bureau() != null) {
      upsertBureauAttribution(article, result);
    }

    if (result.fetchStatus() == FetchStatus.SKIPPED) {
      task.setNotes(result.errorMessage() != null ? result.errorMessage() : "Extraction skipped");
      task.setStatus(EnrichmentTaskStatus.SKIPPED);
    } else if (result.fetchStatus() != FetchStatus.SUCCESS) {
      task.setNotes("Extraction failed: " + result.errorCode());
      task.setStatus(EnrichmentTaskStatus.FAILED);
    } else if (result.classification() == AuthorClassification.BUREAU) {
      task.setNotes("Bureau attribution stored");
      task.setStatus(EnrichmentTaskStatus.DONE);
    } else if (result.classification() == AuthorClassification.PERSON && !result.authors().isEmpty()) {
      boolean inlineResolved = resolveAuthorsInline(article, result);
      if (inlineResolved) {
        task.setNotes("Extraction stored id=" + extraction.getId() + "; resolved " + result.authors().size());
        task.setStatus(result.status() == AuthorExtractionStatus.NEEDS_REVIEW
            ? EnrichmentTaskStatus.NEEDS_REVIEW
            : EnrichmentTaskStatus.DONE);
      } else {
        enqueueResolveTask(article, extraction.getId());
        task.setNotes("Extraction stored id=" + extraction.getId());
        task.setStatus(EnrichmentTaskStatus.DONE);
      }
    } else {
      task.setNotes(result.status() == AuthorExtractionStatus.NEEDS_REVIEW
          ? "Needs review"
          : "No author candidates found");
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
    }
    task.setUpdatedAt(Instant.now());
    safeSaveTask(task, "final");
    logger.info("Author extraction done articleId={} taskStatus={} notes={}",
        article.getId(),
        task.getStatus(),
        task.getNotes());
  }

  private void handleResolveJournalist(EnrichmentTask task) {
    Article article = loadArticle(task);
    if (article == null) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes("Missing article");
      safeSaveTask(task, "missing article");
      return;
    }
    Optional<ArticleAuthorExtraction> extraction = extractionRepository.findFirstByArticleIdOrderByExtractedAtDesc(article.getId());
    if (extraction.isEmpty() || extraction.get().getAuthorRaw() == null) {
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
      task.setNotes("Missing author extraction");
      safeSaveTask(task, "missing extraction");
      logger.info("Journalist resolve skipped articleId={} reason=NO_EXTRACTION",
          article.getId());
      return;
    }
    ArticleAuthorExtraction record = extraction.get();
    if (record.getClassification() == AuthorClassification.BUREAU) {
      task.setStatus(EnrichmentTaskStatus.DONE);
      task.setNotes("Bureau attribution");
      safeSaveTask(task, "bureau resolve");
      return;
    }

    List<AuthorExtractionService.Attribution> authors = parseAttributions(record.getAuthorsJsonb());
    List<String> names = authors.stream().map(AuthorExtractionService.Attribution::name).toList();
    if (names.isEmpty() && record.getAuthorRaw() != null) {
      names = List.of(record.getAuthorRaw());
    }
    List<AuthorNormalizationService.Candidate> normalized = normalizationService.normalizeCandidates(names);
    List<AuthorNormalizationService.Candidate> personCandidates = normalized.stream()
        .filter(candidate -> !candidate.nonPerson())
        .filter(candidate -> !isNonPersonAuthor(article, candidate.name()))
        .toList();

    if (personCandidates.isEmpty()) {
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
      task.setNotes("No resolvable author names");
      safeSaveTask(task, "no resolvable names");
      return;
    }

    int resolvedCount = 0;
    int minConfidence = 100;
    List<Journalist> resolvedJournalists = new ArrayList<>();
    for (AuthorNormalizationService.Candidate candidate : personCandidates) {
      int confidence = authorConfidence(candidate.name(), authors);
      resolutionService.resolve(article, candidate.name(), confidence)
          .map(ArticleJournalist::getJournalist)
          .ifPresent(resolvedJournalists::add);
      resolvedCount++;
      minConfidence = Math.min(minConfidence, confidence);
    }
    resolvedJournalists.forEach(this::enqueueProfileEnrichment);
    task.setStatus(minConfidence < 80 ? EnrichmentTaskStatus.NEEDS_REVIEW : EnrichmentTaskStatus.DONE);
    task.setNotes(minConfidence < 80 ? "Fuzzy match" : "Resolved");
    task.setUpdatedAt(Instant.now());
    safeSaveTask(task, "resolve done");
    logger.info("Journalist resolve done articleId={} author={} status={}",
        article.getId(),
        record.getAuthorRaw(),
        task.getStatus());
  }

  private boolean resolveAuthorsInline(Article article, AuthorExtractionService.ExtractionResult result) {
    if (!properties.isAutoRunAfterIngest()) {
      return false;
    }
    int minConfidence = 100;
    List<Journalist> resolvedJournalists = new ArrayList<>();
    for (AuthorExtractionService.Attribution author : result.authors()) {
      if (author == null || author.name() == null || author.name().isBlank()) {
        continue;
      }
      if (isNonPersonAuthor(article, author.name())) {
        continue;
      }
      int confidence = Math.max(50, author.confidence());
      resolutionService.resolve(article, author.name(), confidence)
          .map(ArticleJournalist::getJournalist)
          .ifPresent(resolvedJournalists::add);
      minConfidence = Math.min(minConfidence, confidence);
    }
    resolvedJournalists.forEach(this::enqueueProfileEnrichment);
    return minConfidence < 100;
  }

  private boolean isNonPersonAuthor(Article article, String name) {
    if (name == null || name.isBlank()) {
      return true;
    }
    String trimmed = name.trim();
    if (normalizationService.isNonPerson(trimmed)) {
      return true;
    }
    String sourceName = article != null ? article.getSourceName() : null;
    if (sourceName != null && !sourceName.isBlank()) {
      String normalizedName = normalizeKey(trimmed);
      String normalizedSource = normalizeKey(sourceName);
      if (!normalizedName.isBlank() && !normalizedSource.isBlank()) {
        if (normalizedName.equals(normalizedSource)
            || normalizedName.contains(normalizedSource)
            || normalizedSource.contains(normalizedName)) {
          return true;
        }
      }
    }
    String domainKey = extractDomainKey(article != null ? article.getSourceUrl() : null);
    if (domainKey != null && !domainKey.isBlank()) {
      String normalizedName = normalizeKey(trimmed);
      if (!normalizedName.isBlank() && normalizedName.contains(domainKey)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeKey(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private String extractDomainKey(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      return null;
    }
    try {
      String host = URI.create(sourceUrl).getHost();
      if (host == null) {
        return null;
      }
      String normalized = host.toLowerCase(Locale.ROOT);
      if (normalized.startsWith("www.")) {
        normalized = normalized.substring(4);
      }
      int dotIndex = normalized.indexOf('.');
      String core = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
      return normalizeKey(core);
    } catch (Exception ex) {
      return null;
    }
  }

  private void enqueueResolveTask(Article article, Long extractionId) {
    EnrichmentTask resolveTask = new EnrichmentTask();
    resolveTask.setTaskType(EnrichmentTaskType.RESOLVE_JOURNALIST);
    resolveTask.setArticle(article);
    resolveTask.setStatus(EnrichmentTaskStatus.PENDING);
    resolveTask.setNextRunAt(Instant.now());
    resolveTask.setNotes(extractionId != null ? "Extraction " + extractionId : null);
    safeSaveTask(resolveTask, "enqueue resolve");
  }

  private void upsertBureauAttribution(Article article, AuthorExtractionService.ExtractionResult result) {
    AuthorExtractionService.Attribution bureau = result.bureau();
    if (bureau == null || bureau.name() == null || bureau.name().isBlank()) {
      return;
    }
    attributionRepository.findFirstByArticleIdAndBureauNameIgnoreCase(article.getId(), bureau.name())
        .orElseGet(() -> {
          ArticleAttribution attribution = new ArticleAttribution();
          attribution.setArticle(article);
          attribution.setBureauName(bureau.name());
          attribution.setAttributionType(AttributionType.BUREAU);
          attribution.setConfidence(bureau.confidence());
          try {
            attribution.setEvidenceJsonb(objectMapper.writeValueAsString(result.evidence()));
          } catch (JsonProcessingException ex) {
            attribution.setEvidenceJsonb("{}");
          }
          return attributionRepository.save(attribution);
        });
  }

  private void handleEnrichProfile(EnrichmentTask task) {
    Journalist journalist = loadJournalist(task);
    if (journalist == null) {
      task.setStatus(EnrichmentTaskStatus.FAILED);
      task.setNotes("Missing journalist");
      safeSaveTask(task, "missing journalist");
      return;
    }
    List<Article> recentArticles = loadRecentArticles(journalist.getId());
    JournalistEnrichmentService.EnrichmentOutcome outcome = journalistEnrichmentService.enrich(journalist, recentArticles);
    logger.info("Journalist enrichment outcome journalistId={} status={} message={}",
        journalist.getId(),
        outcome.queued() ? "QUEUED" : outcome.changed() ? "DONE" : "NO_CHANGES",
        outcome.message());
    if (outcome.queued()) {
      task.setStatus(EnrichmentTaskStatus.NEEDS_REVIEW);
    } else if (outcome.changed()) {
      task.setStatus(EnrichmentTaskStatus.DONE);
    } else if (outcome.message().toLowerCase(Locale.ROOT).contains("no changes")) {
      task.setStatus(EnrichmentTaskStatus.DONE);
    } else {
      task.setStatus(EnrichmentTaskStatus.FAILED);
    }
    task.setNotes(outcome.message());
    task.setUpdatedAt(Instant.now());
    safeSaveTask(task, "enrich profile");
  }

  private Journalist loadJournalist(EnrichmentTask task) {
    Journalist journalist = task.getJournalist();
    if (journalist != null && journalist.getId() != null) {
      return journalistRepository.findById(journalist.getId()).orElse(null);
    }
    return null;
  }

  private List<Article> loadRecentArticles(Long journalistId) {
    if (journalistId == null) {
      return List.of();
    }
    List<Long> articleIds = articleJournalistRepository.findByJournalistId(journalistId).stream()
        .map(link -> link.getArticle() != null ? link.getArticle().getId() : null)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (articleIds.isEmpty()) {
      return List.of();
    }
    return articleRepository.findAllById(articleIds);
  }

  private void enqueueProfileEnrichment(Journalist journalist) {
    if (journalist == null || journalist.getId() == null) {
      return;
    }
    List<EnrichmentTaskStatus> activeStatuses = List.of(
        EnrichmentTaskStatus.PENDING,
        EnrichmentTaskStatus.RUNNING,
        EnrichmentTaskStatus.NEEDS_REVIEW
    );
    Optional<EnrichmentTask> existing = taskRepository
        .findTopByJournalistIdAndStatusInOrderByCreatedAtDesc(journalist.getId(), activeStatuses);
    if (existing.isPresent()) {
      return;
    }
    EnrichmentTask task = new EnrichmentTask();
    task.setTaskType(EnrichmentTaskType.ENRICH_PROFILE);
    task.setJournalist(journalist);
    task.setStatus(EnrichmentTaskStatus.PENDING);
    task.setNextRunAt(Instant.now());
    task.setNotes("Auto enrichment");
    safeSaveTask(task, "enqueue profile");
  }

  private List<AuthorExtractionService.Attribution> parseAttributions(String jsonb) {
    if (jsonb == null || jsonb.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(jsonb, new TypeReference<List<AuthorExtractionService.Attribution>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private int authorConfidence(String name, List<AuthorExtractionService.Attribution> attributions) {
    if (name == null) {
      return 70;
    }
    for (AuthorExtractionService.Attribution attribution : attributions) {
      if (attribution.name() != null && attribution.name().equalsIgnoreCase(name)) {
        return Math.max(50, attribution.confidence());
      }
    }
    return 70;
  }

  private void safeSaveTask(EnrichmentTask task, String reason) {
    try {
      taskRepository.save(task);
    } catch (ObjectOptimisticLockingFailureException ex) {
      logger.info("Enrichment task save skipped reason=CONCURRENT_UPDATE taskId={} detail={}",
          task.getId(), reason);
    }
  }

  private Article loadArticle(EnrichmentTask task) {
    if (task == null || task.getArticle() == null) {
      return null;
    }
    Long articleId = task.getArticle().getId();
    if (articleId == null) {
      return null;
    }
    return articleRepository.findWithBeatById(articleId).orElse(null);
  }
}
