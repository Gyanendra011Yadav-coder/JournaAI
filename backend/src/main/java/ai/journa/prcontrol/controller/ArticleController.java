package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.IngestMode;
import ai.journa.prcontrol.domain.LensSource;
import ai.journa.prcontrol.domain.Role;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.ArticleListResponse;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.service.CacheKeyService;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.ArticleResponseFactory;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.AppLocaleResolver;
import ai.journa.prcontrol.service.QueryBuilderService;
import ai.journa.prcontrol.repository.UserProfileRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
  private final ArticleService articleService;
  private final CurrentUserService currentUserService;
  private final NewsProviderProperties newsProviderProperties;
  private final AppLocaleResolver localeResolver;
  private final CacheKeyService cacheKeyService;
  private final UserProfileRepository userProfileRepository;
  private final QueryBuilderService queryBuilderService;
  private final BeatRepository beatRepository;
  private final ArticleResponseFactory articleResponseFactory;

  public ArticleController(ArticleService articleService,
                           CurrentUserService currentUserService,
                           NewsProviderProperties newsProviderProperties,
                           AppLocaleResolver localeResolver,
                           CacheKeyService cacheKeyService,
                           UserProfileRepository userProfileRepository,
                           QueryBuilderService queryBuilderService,
                           BeatRepository beatRepository,
                           ArticleResponseFactory articleResponseFactory) {
    this.articleService = articleService;
    this.currentUserService = currentUserService;
    this.newsProviderProperties = newsProviderProperties;
    this.localeResolver = localeResolver;
    this.cacheKeyService = cacheKeyService;
    this.userProfileRepository = userProfileRepository;
    this.queryBuilderService = queryBuilderService;
    this.beatRepository = beatRepository;
    this.articleResponseFactory = articleResponseFactory;
  }

  @GetMapping
  public ArticleListResponse search(@RequestParam(defaultValue = "SEARCH") IngestMode mode,
                                    @RequestParam(required = false) String lens,
                                    @RequestParam(required = false) Long beatId,
                                    @RequestParam(required = false) Long journalistId,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(required = false) ArticleStatus status,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) int size,
                                    HttpServletRequest servletRequest) {
    User actor = currentUserService.requireCurrentUser();
    ArticleStatus effectiveStatus = actor.getRole() == Role.ADMIN ? status : ArticleStatus.PUBLISHED;
    LensSource lensSource = resolveLens(mode, lens);
    AppLocaleResolver.Resolution locale = localeResolver.resolve(actor, servletRequest);

    ArticleListResponse response = new ArticleListResponse();
    if (journalistId != null) {
      Page<Article> results = articleService.searchArticlesByJournalist(
          actor,
          journalistId,
          effectiveStatus,
          parseInstant(from),
          parseInstant(to),
          page,
          size,
          newsProviderProperties.getSearchesPerMinute()
      );
      response.setItems(results.getContent().stream().map(this::toResponse).toList());
      response.setTotal(results.getTotalElements());
      response.setPage(results.getNumber());
      response.setSize(results.getSize());
    } else if (mode == IngestMode.TRENDING && (lens == null || lens.equalsIgnoreCase("MIX") || lens.equalsIgnoreCase("ALL"))) {
      List<Article> merged = mergeTrendingMix(actor, category, effectiveStatus, parseInstant(from), parseInstant(to), size);
      response.setItems(merged.stream().map(this::toResponse).toList());
      response.setTotal(merged.size());
      response.setPage(0);
      response.setSize(merged.size());
    } else {
      String searchCountry = null;
      if (mode == IngestMode.SEARCH && beatId == null && category == null) {
        searchCountry = locale.country();
      }
      Page<Article> results = articleService.searchArticles(
          actor,
          beatId,
          category,
          lensSource,
          effectiveStatus,
          searchCountry,
          parseInstant(from),
          parseInstant(to),
          page,
          size,
          newsProviderProperties.getSearchesPerMinute()
      );
      response.setItems(results.getContent().stream().map(this::toResponse).toList());
      response.setTotal(results.getTotalElements());
      response.setPage(results.getNumber());
      response.setSize(results.getSize());
    }

    String cacheLens = lens;
    if (journalistId != null) {
      return response;
    }
    if (mode == IngestMode.TRENDING && (lens == null || lens.equalsIgnoreCase("MIX") || lens.equalsIgnoreCase("ALL"))) {
      cacheLens = "LOCAL";
    }
    String cacheQuery = null;
    if (mode == IngestMode.SEARCH && beatId != null) {
      Beat beat = beatRepository.findById(beatId).orElse(null);
      if (beat != null) {
        QueryBuilderService.QueryBundle bundle = queryBuilderService.buildSearchQueries(beat, actor);
        cacheQuery = "CLIENT".equalsIgnoreCase(cacheLens) ? bundle.clientQuery() : bundle.beatQuery();
      }
    }
    String cacheKey = cacheKeyService.build(
        mode,
        cacheLens != null ? cacheLens : "ALL",
        beatId,
        category,
        cacheQuery,
        locale
    );
    articleService.getLastRefreshedAt(cacheKey).ifPresent(last -> response.setLastRefreshedAt(last.toString()));
    articleService.isStaleCache(cacheKey).ifPresent(response::setStaleCache);
    return response;
  }

  @GetMapping("/{id}")
  public ArticleResponse getArticle(@PathVariable Long id) {
    User actor = currentUserService.requireCurrentUser();
    Article article = articleService.getArticle(id, actor);
    if (actor.getRole() != Role.ADMIN && article.getStatus() != ArticleStatus.PUBLISHED) {
      throw new IllegalStateException("Article not published");
    }
    return toResponse(article);
  }

  private ArticleResponse toResponse(Article article) {
    return articleResponseFactory.toResponse(article);
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid timestamp format");
    }
  }

  private LensSource resolveLens(IngestMode mode, String lens) {
    if (mode == IngestMode.TRENDING) {
      if ("LOCAL".equalsIgnoreCase(lens)) {
        return LensSource.TRENDING_LOCAL;
      }
      if ("GLOBAL".equalsIgnoreCase(lens)) {
        return LensSource.TRENDING_GLOBAL;
      }
      return null;
    }
    if ("CLIENT".equalsIgnoreCase(lens)) {
      return LensSource.CLIENT;
    }
    if ("BEAT".equalsIgnoreCase(lens)) {
      return LensSource.BEAT;
    }
    return null;
  }

  private List<Article> mergeTrendingMix(User actor,
                                         String category,
                                         ArticleStatus status,
                                         Instant from,
                                         Instant to,
                                         int size) {
    int localRatio = userProfileRepository.findById(actor.getId())
        .map(profile -> profile.getTrendingLocalRatio())
        .orElse(40);
    int localCount = Math.max(1, (int) Math.round(size * (localRatio / 100.0)));
    int globalCount = Math.max(1, size - localCount);

    Page<Article> localPage = articleService.searchArticles(
        actor,
        null,
        category,
        LensSource.TRENDING_LOCAL,
        status,
        localeResolver.resolveDefaults().country(),
        from,
        to,
        0,
        size,
        newsProviderProperties.getSearchesPerMinute()
    );
    Page<Article> globalPage = articleService.searchArticles(
        actor,
        null,
        category,
        LensSource.TRENDING_GLOBAL,
        status,
        null,
        from,
        to,
        0,
        size,
        newsProviderProperties.getSearchesPerMinute()
    );

    List<Article> merged = new ArrayList<>();
    Set<String> seenUrls = new java.util.HashSet<>();
    addWithLimit(merged, seenUrls, localPage.getContent(), localCount);
    addWithLimit(merged, seenUrls, globalPage.getContent(), globalCount);
    return merged;
  }

  private void addWithLimit(List<Article> target, Set<String> seenUrls, List<Article> source, int limit) {
    for (Article article : source) {
      if (target.size() >= limit) {
        break;
      }
      if (article.getUrl() != null && seenUrls.add(article.getUrl())) {
        target.add(article);
      }
    }
  }
}
