package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleStatus;
import ai.journa.prcontrol.domain.Role;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.ArticleListResponse;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.CurrentUserService;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
  private final ArticleService articleService;
  private final CurrentUserService currentUserService;
  private final NewsProviderProperties newsProviderProperties;

  public ArticleController(ArticleService articleService,
                           CurrentUserService currentUserService,
                           NewsProviderProperties newsProviderProperties) {
    this.articleService = articleService;
    this.currentUserService = currentUserService;
    this.newsProviderProperties = newsProviderProperties;
  }

  @GetMapping
  public ArticleListResponse search(@RequestParam(required = false) Long beatId,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(required = false) ArticleStatus status,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) int size) {
    User actor = currentUserService.requireCurrentUser();
    ArticleStatus effectiveStatus = actor.getRole() == Role.ADMIN ? status : ArticleStatus.PUBLISHED;
    Page<Article> results = articleService.searchArticles(
        actor,
        beatId,
        effectiveStatus,
        parseInstant(from),
        parseInstant(to),
        page,
        size,
        newsProviderProperties.getSearchesPerMinute()
    );

    ArticleListResponse response = new ArticleListResponse();
    response.setItems(results.getContent().stream().map(this::toResponse).toList());
    response.setTotal(results.getTotalElements());
    response.setPage(results.getNumber());
    response.setSize(results.getSize());
    articleService.getLastRefreshedAt(beatId).ifPresent(last -> response.setLastRefreshedAt(last.toString()));
    response.setStaleCache(false);
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
    response.setBeatId(article.getBeat().getId());
    response.setBeatName(article.getBeat().getName());
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
}
