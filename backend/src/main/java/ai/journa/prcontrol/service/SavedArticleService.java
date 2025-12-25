package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.SavedArticle;
import ai.journa.prcontrol.domain.SavedArticleId;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.ArticleResponse;
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

  public SavedArticleService(SavedArticleRepository savedArticleRepository, ArticleRepository articleRepository) {
    this.savedArticleRepository = savedArticleRepository;
    this.articleRepository = articleRepository;
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
          ArticleResponse article = new ArticleResponse();
          Article entity = saved.getArticle();
          article.setId(entity.getId());
          article.setProviderType(entity.getProviderType().name());
          article.setProviderArticleId(entity.getProviderArticleId());
          article.setBeatId(entity.getBeat() != null ? entity.getBeat().getId() : null);
          article.setBeatName(entity.getBeat() != null ? entity.getBeat().getName() : null);
          article.setCategory(entity.getCategory());
          article.setLensSource(entity.getLensSource().name());
          article.setClientMatch(entity.isClientMatch());
          article.setTitle(entity.getTitle());
          article.setDescription(entity.getDescription());
          article.setContent(entity.getContent());
          article.setUrl(entity.getUrl());
          article.setImageUrl(entity.getImageUrl());
          article.setPublishedAtUtc(entity.getProviderPublishedAtUtc() != null ? entity.getProviderPublishedAtUtc().toString() : null);
          article.setLang(entity.getLang());
          article.setSourceId(entity.getSourceId());
          article.setSourceName(entity.getSourceName());
          article.setSourceUrl(entity.getSourceUrl());
          article.setSourceCountry(entity.getSourceCountry());
          article.setFetchedAtUtc(entity.getFetchedAtUtc() != null ? entity.getFetchedAtUtc().toString() : null);
          article.setStatus(entity.getStatus().name());
          article.setPublishedBy(entity.getPublishedBy() != null ? entity.getPublishedBy().getEmail() : null);
          article.setInternalPublishedAtUtc(entity.getInternalPublishedAtUtc() != null ? entity.getInternalPublishedAtUtc().toString() : null);
          response.setArticle(article);
          return response;
        }).toList();
  }
}
