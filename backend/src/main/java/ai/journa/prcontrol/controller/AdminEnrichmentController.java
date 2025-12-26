package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.EnrichmentTask;
import ai.journa.prcontrol.domain.EnrichmentTaskStatus;
import ai.journa.prcontrol.dto.EnrichmentTaskResponse;
import ai.journa.prcontrol.repository.EnrichmentTaskRepository;
import ai.journa.prcontrol.service.EnrichmentTaskRunner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/enrichment")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEnrichmentController {
  private final EnrichmentTaskRepository taskRepository;
  private final EnrichmentTaskRunner taskRunner;

  public AdminEnrichmentController(EnrichmentTaskRepository taskRepository, EnrichmentTaskRunner taskRunner) {
    this.taskRepository = taskRepository;
    this.taskRunner = taskRunner;
  }

  @GetMapping("/tasks")
  public List<EnrichmentTaskResponse> listTasks(@RequestParam(required = false) EnrichmentTaskStatus status) {
    List<EnrichmentTask> tasks = status != null ? taskRepository.findByStatus(status) : taskRepository.findAll();
    return tasks.stream().map(this::toResponse).toList();
  }

  @PostMapping("/run")
  public EnrichmentTaskResponse runTask(@RequestParam(required = false) Long articleId,
                                        @RequestParam(required = false) Long journalistId) {
    if (articleId == null && journalistId == null) {
      taskRunner.runPendingTasks();
      EnrichmentTaskResponse response = new EnrichmentTaskResponse();
      response.setStatus("RUN_TRIGGERED");
      return response;
    }
    EnrichmentTask task = taskRunner.runOnDemand(articleId, journalistId);
    taskRunner.runPendingTasks();
    return toResponse(task);
  }

  private EnrichmentTaskResponse toResponse(EnrichmentTask task) {
    EnrichmentTaskResponse response = new EnrichmentTaskResponse();
    response.setId(task.getId());
    response.setTaskType(task.getTaskType().name());
    response.setArticleId(task.getArticle() != null ? task.getArticle().getId() : null);
    if (task.getArticle() != null) {
      response.setArticleTitle(task.getArticle().getTitle());
      response.setArticleUrl(task.getArticle().getUrl());
    }
    response.setJournalistId(task.getJournalist() != null ? task.getJournalist().getId() : null);
    if (task.getJournalist() != null) {
      response.setJournalistName(task.getJournalist().getFullName());
    }
    response.setStatus(task.getStatus().name());
    response.setPriority(task.getPriority());
    response.setAttempts(task.getAttempts());
    response.setNextRunAt(task.getNextRunAt() != null ? task.getNextRunAt().toString() : null);
    response.setNotes(task.getNotes());
    return response;
  }
}
