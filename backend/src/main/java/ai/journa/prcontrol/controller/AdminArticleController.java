package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.ArticleResponseFactory;
import ai.journa.prcontrol.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {
  private final ArticleService articleService;
  private final CurrentUserService currentUserService;
  private final ArticleResponseFactory articleResponseFactory;

  public AdminArticleController(ArticleService articleService,
                                CurrentUserService currentUserService,
                                ArticleResponseFactory articleResponseFactory) {
    this.articleService = articleService;
    this.currentUserService = currentUserService;
    this.articleResponseFactory = articleResponseFactory;
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

  @PostMapping("/manual")
  public ArticleResponse createManual(@Valid @RequestBody ManualArticleRequest request) {
    Article article = articleService.createManualArticle(request, currentUserService.requireCurrentUser());
    return toResponse(article);
  }

  private ArticleResponse toResponse(Article article) {
    return articleResponseFactory.toResponse(article);
  }
}
