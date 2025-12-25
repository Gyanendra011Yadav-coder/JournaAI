package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.IngestMode;
import ai.journa.prcontrol.dto.RefreshResponse;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.IngestionService;
import ai.journa.prcontrol.service.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {
  private final IngestionService ingestionService;
  private final CurrentUserService currentUserService;
  private final LocaleResolver localeResolver;

  public IngestController(IngestionService ingestionService, CurrentUserService currentUserService, LocaleResolver localeResolver) {
    this.ingestionService = ingestionService;
    this.currentUserService = currentUserService;
    this.localeResolver = localeResolver;
  }

  @PostMapping("/refresh")
  public RefreshResponse refresh(@RequestParam(defaultValue = "SEARCH") IngestMode mode,
                                 @RequestParam(required = false) Long beatId,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String lensOrTrack,
                                 @RequestParam(required = false) String q,
                                 HttpServletRequest servletRequest) {
    var actor = currentUserService.requireCurrentUser();
    String resolvedLens = lensOrTrack != null ? lensOrTrack : (mode == IngestMode.TRENDING ? "LOCAL" : "BEAT");
    var locale = localeResolver.resolve(actor, servletRequest);
    IngestionService.IngestRequest request = new IngestionService.IngestRequest(mode, beatId, category, resolvedLens, q, actor);
    IngestionService.RefreshResult result = ingestionService.refresh(request, locale, actor, true);
    RefreshResponse response = new RefreshResponse();
    response.setLastRefreshedAt(result.getLastSuccessAt() != null ? result.getLastSuccessAt().toString() : null);
    response.setResolvedCountry(locale.country());
    response.setResolvedLang(locale.lang());
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
