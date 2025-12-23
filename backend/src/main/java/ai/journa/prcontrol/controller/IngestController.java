package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.RefreshResponse;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {
  private final IngestionService ingestionService;
  private final CurrentUserService currentUserService;

  public IngestController(IngestionService ingestionService, CurrentUserService currentUserService) {
    this.ingestionService = ingestionService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/refresh")
  public RefreshResponse refresh(@RequestParam Long beatId) {
    IngestionService.RefreshResult result = ingestionService.refreshBeat(beatId, currentUserService.requireCurrentUser(), true);
    RefreshResponse response = new RefreshResponse();
    response.setLastRefreshedAt(result.getLastSuccessAt() != null ? result.getLastSuccessAt().toString() : null);
    if (result.isSuccess()) {
      response.setStatus("SUCCESS");
      response.setStaleCache(false);
      return response;
    }
    if (result.isSkipped()) {
      response.setStatus("SKIPPED");
      response.setStaleCache(true);
      response.setMessage(result.getMessage());
      return response;
    }
    response.setStatus("FAILED");
    response.setStaleCache(true);
    response.setMessage(result.getMessage());
    return response;
  }
}
