package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.dto.ArticleSearchRequest;
import ai.journa.prcontrol.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/search")
    public ResponseEntity<List<ArticleResponse>> search(@Valid @RequestBody ArticleSearchRequest request,
                                                        Authentication authentication) {
        List<Article> articles = articleService.searchArticles(authentication.getName(), request);
        return ResponseEntity.ok(articles.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> get(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toResponse(articleService.getArticle(id, authentication.getName())));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<ArticleResponse> save(@PathVariable Long id,
                                                @RequestParam boolean saved,
                                                Authentication authentication) {
        return ResponseEntity.ok(toResponse(articleService.saveArticle(id, saved, authentication.getName())));
    }

    private ArticleResponse toResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setHeadline(article.getHeadline());
        response.setSource(article.getSource());
        response.setAuthor(article.getAuthor());
        response.setUrl(article.getUrl());
        response.setPublishedAt(article.getPublishedAt());
        response.setSummary(article.getSummary());
        response.setSaved(article.isSaved());
        return response;
    }
}
