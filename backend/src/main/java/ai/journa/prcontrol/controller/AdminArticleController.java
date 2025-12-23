package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {
  private final ArticleService articleService;
  private final CurrentUserService currentUserService;

  public AdminArticleController(ArticleService articleService, CurrentUserService currentUserService) {
    this.articleService = articleService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/{id}/publish")
  public ArticleResponse publish(@PathVariable Long id) {
    Article article = articleService.publishArticle(id, currentUserService.requireCurrentUser());
    return toResponse(article);
  }

  @PostMapping("/{id}/unpublish")
  public ArticleResponse unpublish(@PathVariable Long id) {
    Article article = articleService.unpublishArticle(id, currentUserService.requireCurrentUser());
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
}
