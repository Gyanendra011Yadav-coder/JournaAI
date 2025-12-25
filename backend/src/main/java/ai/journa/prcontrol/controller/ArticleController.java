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
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.LocaleResolver;
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
  private final LocaleResolver localeResolver;
  private final CacheKeyService cacheKeyService;
  private final UserProfileRepository userProfileRepository;
  private final QueryBuilderService queryBuilderService;
  private final BeatRepository beatRepository;

  public ArticleController(ArticleService articleService,
                           CurrentUserService currentUserService,
                           NewsProviderProperties newsProviderProperties,
                           LocaleResolver localeResolver,
                           CacheKeyService cacheKeyService,
                           UserProfileRepository userProfileRepository,
                           QueryBuilderService queryBuilderService,
                           BeatRepository beatRepository) {
    this.articleService = articleService;
    this.currentUserService = currentUserService;
    this.newsProviderProperties = newsProviderProperties;
    this.localeResolver = localeResolver;
    this.cacheKeyService = cacheKeyService;
    this.userProfileRepository = userProfileRepository;
    this.queryBuilderService = queryBuilderService;
    this.beatRepository = beatRepository;
  }

  @GetMapping
  public ArticleListResponse search(@RequestParam(defaultValue = "SEARCH") IngestMode mode,
                                    @RequestParam(required = false) String lens,
                                    @RequestParam(required = false) Long beatId,
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
    LocaleResolver.Resolution locale = localeResolver.resolve(actor, servletRequest);

    ArticleListResponse response = new ArticleListResponse();
    if (mode == IngestMode.TRENDING && (lens == null || lens.equalsIgnoreCase("MIX") || lens.equalsIgnoreCase("ALL"))) {
      List<Article> merged = mergeTrendingMix(actor, category, effectiveStatus, parseInstant(from), parseInstant(to), size);
      response.setItems(merged.stream().map(this::toResponse).toList());
      response.setTotal(merged.size());
      response.setPage(0);
      response.setSize(merged.size());
    } else {
      Page<Article> results = articleService.searchArticles(
          actor,
          beatId,
          category,
          lensSource,
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
    }

    String cacheLens = lens;
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
    ArticleResponse response = new ArticleResponse();
    response.setId(article.getId());
    response.setProviderType(article.getProviderType().name());
    response.setProviderArticleId(article.getProviderArticleId());
    response.setBeatId(article.getBeat() != null ? article.getBeat().getId() : null);
    response.setBeatName(article.getBeat() != null ? article.getBeat().getName() : null);
    response.setCategory(article.getCategory());
    response.setLensSource(article.getLensSource().name());
    response.setClientMatch(article.isClientMatch());
    response.setTitle(article.getTitle());
    response.setDescription(article.getDescription());
    response.setContent(article.getContent());
    response.setUrl(article.getUrl());
    response.setImageUrl(article.getImageUrl());
    response.setPublishedAtUtc(article.getProviderPublishedAtUtc() != null ? article.getProviderPublishedAtUtc().toString() : null);
    response.setLang(article.getLang());
    response.setSourceId(article.getSourceId());
    response.setSourceName(article.getSourceName());
    response.setSourceUrl(article.getSourceUrl());
    response.setSourceCountry(article.getSourceCountry());
    response.setFetchedAtUtc(article.getFetchedAtUtc() != null ? article.getFetchedAtUtc().toString() : null);
    response.setStatus(article.getStatus().name());
    response.setPublishedBy(article.getPublishedBy() != null ? article.getPublishedBy().getEmail() : null);
    response.setInternalPublishedAtUtc(article.getInternalPublishedAtUtc() != null ? article.getInternalPublishedAtUtc().toString() : null);
    return response;
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
