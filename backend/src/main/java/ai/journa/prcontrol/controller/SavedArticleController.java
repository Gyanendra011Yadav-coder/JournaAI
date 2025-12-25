package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.SavedArticleResponse;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.SavedArticleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SavedArticleController {
  private final SavedArticleService savedArticleService;
  private final CurrentUserService currentUserService;

  public SavedArticleController(SavedArticleService savedArticleService, CurrentUserService currentUserService) {
    this.savedArticleService = savedArticleService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/api/articles/{id}/save")
  public void save(@PathVariable Long id) {
    savedArticleService.save(currentUserService.requireCurrentUser(), id);
  }

  @PostMapping("/api/articles/{id}/pin")
  public void pin(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean pinned) {
    savedArticleService.pin(currentUserService.requireCurrentUser(), id, pinned);
  }

  @GetMapping("/api/saved-articles")
  public List<SavedArticleResponse> list() {
    return savedArticleService.list(currentUserService.requireCurrentUser());
  }
}
