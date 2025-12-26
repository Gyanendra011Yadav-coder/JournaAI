package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import org.springframework.stereotype.Service;

@Service
public class ArticleResponseFactory {
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ArticleJournalistRepository articleJournalistRepository;

  public ArticleResponseFactory(ArticleAuthorExtractionRepository extractionRepository,
                                ArticleJournalistRepository articleJournalistRepository) {
    this.extractionRepository = extractionRepository;
    this.articleJournalistRepository = articleJournalistRepository;
  }

  public ArticleResponse toResponse(Article article) {
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
    populateAuthorFields(article, response);
    return response;
  }

  private void populateAuthorFields(Article article, ArticleResponse response) {
    if (article.getId() == null) {
      return;
    }
    extractionRepository.findFirstByArticleIdOrderByExtractedAtDesc(article.getId())
        .map(ArticleAuthorExtraction::getAuthorRaw)
        .filter(value -> value != null && !value.isBlank())
        .ifPresent(response::setAuthorRaw);

    articleJournalistRepository.findFirstByArticleIdOrderByMatchConfidenceDesc(article.getId())
        .map(ArticleJournalist::getJournalist)
        .ifPresent(journalist -> applyJournalist(response, journalist));
  }

  private void applyJournalist(ArticleResponse response, Journalist journalist) {
    if (journalist == null) {
      return;
    }
    response.setJournalistId(journalist.getId());
    response.setJournalistName(journalist.getFullName());
    if (response.getAuthorRaw() == null || response.getAuthorRaw().isBlank()) {
      response.setAuthorRaw(journalist.getFullName());
    }
  }
}
