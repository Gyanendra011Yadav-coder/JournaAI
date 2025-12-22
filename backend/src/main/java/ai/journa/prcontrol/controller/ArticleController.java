package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.dto.ArticleSearchResponse;
import ai.journa.prcontrol.dto.ManualArticleRequest;
import ai.journa.prcontrol.dto.RefreshRequest;
import ai.journa.prcontrol.dto.RefreshResponse;
import ai.journa.prcontrol.service.ArticleService;
import ai.journa.prcontrol.service.NewsIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final NewsIngestionService newsIngestionService;

    public ArticleController(ArticleService articleService,
                             NewsIngestionService newsIngestionService) {
        this.articleService = articleService;
        this.newsIngestionService = newsIngestionService;
    }

    @GetMapping
    public ResponseEntity<ArticleSearchResponse> search(@RequestParam String beat,
                                                        @RequestParam String timeframe,
                                                        @RequestParam(required = false) Instant from,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        Authentication authentication) {
        Page<Article> articles = articleService.searchArticles(authentication.getName(), beat, timeframe, from, page, size);
        ArticleSearchResponse response = new ArticleSearchResponse();
        response.setItems(articles.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotal(articles.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setLastRefreshedAt(articleService.getLastRefresh(beat, timeframe).orElse(null));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(newsIngestionService.refresh(authentication.getName(), request));
    }

    @PostMapping("/manual")
    public ResponseEntity<ArticleResponse> manual(@Valid @RequestBody ManualArticleRequest request,
                                                  Authentication authentication) {
        Article article = newsIngestionService.addManualArticle(authentication.getName(), request);
        return ResponseEntity.ok(toResponse(article));
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
        response.setCanonicalUrl(article.getCanonicalUrl());
        response.setUrl(article.getUrl());
        response.setPublishedAt(article.getPublishedAt());
        response.setSummary(article.getSummary());
        response.setProvider(article.getProvider());
        response.setBeats(article.getBeats().stream().map(beat -> beat.getName()).collect(Collectors.toList()));
        response.setSaved(article.isSaved());
        return response;
    }
}
