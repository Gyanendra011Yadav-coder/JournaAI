package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.IngestMode;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.BeatResponse;
import ai.journa.prcontrol.dto.BeatStatusResponse;
import ai.journa.prcontrol.service.AppLocaleResolver;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.BeatService;
import ai.journa.prcontrol.service.CacheKeyService;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.QueryBuilderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beats")
public class BeatController {
  private final BeatService beatService;
  private final CurrentUserService currentUserService;
  private final CacheKeyService cacheKeyService;
  private final QueryBuilderService queryBuilderService;
  private final AppLocaleResolver localeResolver;
  private final ArticleService articleService;

  public BeatController(BeatService beatService,
                        CurrentUserService currentUserService,
                        CacheKeyService cacheKeyService,
                        QueryBuilderService queryBuilderService,
                        AppLocaleResolver localeResolver,
                        ArticleService articleService) {
    this.beatService = beatService;
    this.currentUserService = currentUserService;
    this.cacheKeyService = cacheKeyService;
    this.queryBuilderService = queryBuilderService;
    this.localeResolver = localeResolver;
    this.articleService = articleService;
  }

  @GetMapping
  public List<BeatResponse> listBeats() {
    return beatService.findAllActive().stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/status")
  public List<BeatStatusResponse> listBeatStatus(HttpServletRequest servletRequest) {
    User actor = currentUserService.requireCurrentUser();
    AppLocaleResolver.Resolution locale = localeResolver.resolve(actor, servletRequest);
    return beatService.findAllActive().stream()
        .map(beat -> {
          QueryBuilderService.QueryBundle bundle = queryBuilderService.buildSearchQueries(beat, actor);
          String cacheKey = cacheKeyService.build(
              IngestMode.SEARCH,
              "BEAT",
              beat.getId(),
              null,
              bundle.beatQuery(),
              locale
          );
          String lastRefreshedAt = articleService.getLastRefreshedAt(cacheKey)
              .map(instant -> instant.toString())
              .orElse(null);
          return new BeatStatusResponse(beat.getId(), beat.getName(), lastRefreshedAt);
        })
        .toList();
  }

  private BeatResponse toResponse(Beat beat) {
    return new BeatResponse(beat.getId(), beat.getName(), beat.getSlug(), beat.isActive());
  }
}
