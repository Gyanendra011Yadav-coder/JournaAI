package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.SavedArticle;
import ai.journa.prcontrol.domain.SavedArticleId;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.SavedArticleResponse;
import ai.journa.prcontrol.repository.ArticleRepository;
import ai.journa.prcontrol.repository.SavedArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SavedArticleService {
  private final SavedArticleRepository savedArticleRepository;
  private final ArticleRepository articleRepository;
  private final ArticleResponseFactory articleResponseFactory;

  public SavedArticleService(SavedArticleRepository savedArticleRepository,
                             ArticleRepository articleRepository,
                             ArticleResponseFactory articleResponseFactory) {
    this.savedArticleRepository = savedArticleRepository;
    this.articleRepository = articleRepository;
    this.articleResponseFactory = articleResponseFactory;
  }

  @Transactional
  public void save(User user, Long articleId) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    SavedArticleId id = new SavedArticleId(user.getId(), articleId);
    SavedArticle saved = savedArticleRepository.findById(id).orElseGet(() -> {
      SavedArticle entity = new SavedArticle();
      entity.setUser(user);
      entity.setArticle(article);
      return entity;
    });
    saved.setSavedAt(Instant.now());
    savedArticleRepository.save(saved);
  }

  @Transactional
  public void pin(User user, Long articleId, boolean pinned) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new IllegalStateException("Article not found"));
    SavedArticleId id = new SavedArticleId(user.getId(), articleId);
    SavedArticle saved = savedArticleRepository.findById(id).orElseGet(() -> {
      SavedArticle entity = new SavedArticle();
      entity.setUser(user);
      entity.setArticle(article);
      return entity;
    });
    saved.setPinned(pinned);
    saved.setSavedAt(Instant.now());
    savedArticleRepository.save(saved);
  }

  public List<SavedArticleResponse> list(User user) {
    return savedArticleRepository.findByUser_Id(user.getId()).stream()
        .map(saved -> {
          SavedArticleResponse response = new SavedArticleResponse();
          response.setArticleId(saved.getArticle().getId());
          response.setPinned(saved.isPinned());
          response.setSavedAt(saved.getSavedAt() != null ? saved.getSavedAt().toString() : null);
          response.setNote(saved.getNote());
          response.setTags(saved.getTags());
          Article entity = saved.getArticle();
          response.setArticle(articleResponseFactory.toResponse(entity));
          return response;
        }).toList();
  }
}
