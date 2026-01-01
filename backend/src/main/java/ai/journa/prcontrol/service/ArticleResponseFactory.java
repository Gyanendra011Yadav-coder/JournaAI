package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleAuthorExtraction;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.AuthorClassification;
import ai.journa.prcontrol.domain.AuthorExtractionStatus;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.dto.ArticleResponse;
import ai.journa.prcontrol.repository.ArticleAuthorExtractionRepository;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.EnrichmentTaskRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;

@Service
public class ArticleResponseFactory {
  private final ArticleAuthorExtractionRepository extractionRepository;
  private final ArticleJournalistRepository articleJournalistRepository;
  private final EnrichmentTaskRepository taskRepository;
  private final AuthorNormalizationService normalizationService;

  public ArticleResponseFactory(ArticleAuthorExtractionRepository extractionRepository,
                                ArticleJournalistRepository articleJournalistRepository,
                                EnrichmentTaskRepository taskRepository,
                                AuthorNormalizationService normalizationService) {
    this.extractionRepository = extractionRepository;
    this.articleJournalistRepository = articleJournalistRepository;
    this.taskRepository = taskRepository;
    this.normalizationService = normalizationService;
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
    populateAuthorTaskStatus(article, response);
    return response;
  }

  private void populateAuthorFields(Article article, ArticleResponse response) {
    if (article.getId() == null) {
      return;
    }
    boolean[] bureauLike = new boolean[] { false };
    extractionRepository.findFirstByArticleIdOrderByExtractedAtDesc(article.getId())
        .filter(extraction -> extraction.getStatus() == AuthorExtractionStatus.SUCCESS)
        .ifPresent(extraction -> {
          boolean sourceMatch = isSourceMatch(article, extraction.getAuthorRaw());
          if (extraction.getClassification() == AuthorClassification.BUREAU || sourceMatch) {
            response.setAuthorRaw("Bureau");
            bureauLike[0] = true;
            return;
          }
          String authorRaw = extraction.getAuthorRaw();
          if (authorRaw != null && !authorRaw.isBlank()) {
            response.setAuthorRaw(authorRaw);
          }
        });

    if (bureauLike[0]) {
      return;
    }
    articleJournalistRepository.findFirstByArticleIdOrderByMatchConfidenceDesc(article.getId())
        .map(ArticleJournalist::getJournalist)
        .ifPresent(journalist -> applyJournalist(response, journalist, article));
  }

  private void applyJournalist(ArticleResponse response, Journalist journalist, Article article) {
    if (journalist == null) {
      return;
    }
    String name = journalist.getFullName();
    if (name == null || name.isBlank()) {
      return;
    }
    if (normalizationService.isNonPerson(name) || isSourceMatch(article, name)) {
      return;
    }
    response.setJournalistId(journalist.getId());
    response.setJournalistName(name);
    if (response.getAuthorRaw() == null || response.getAuthorRaw().isBlank()) {
      response.setAuthorRaw(name);
    }
  }

  private boolean isSourceMatch(Article article, String authorRaw) {
    if (authorRaw == null || authorRaw.isBlank()) {
      return false;
    }
    String sourceName = article != null ? article.getSourceName() : null;
    if (sourceName != null && !sourceName.isBlank()) {
      String normalizedName = normalizeKey(authorRaw);
      String normalizedSource = normalizeKey(sourceName);
      if (!normalizedName.isBlank() && !normalizedSource.isBlank()) {
        if (normalizedName.equals(normalizedSource)
            || normalizedName.contains(normalizedSource)
            || normalizedSource.contains(normalizedName)) {
          return true;
        }
      }
    }
    String domainKey = extractDomainKey(article != null ? article.getSourceUrl() : null);
    if (domainKey != null && !domainKey.isBlank()) {
      String normalizedName = normalizeKey(authorRaw);
      if (!normalizedName.isBlank() && normalizedName.contains(domainKey)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeKey(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private String extractDomainKey(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      return null;
    }
    try {
      String host = URI.create(sourceUrl).getHost();
      if (host == null) {
        return null;
      }
      String normalized = host.toLowerCase(Locale.ROOT);
      if (normalized.startsWith("www.")) {
        normalized = normalized.substring(4);
      }
      int dotIndex = normalized.indexOf('.');
      String core = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
      return normalizeKey(core);
    } catch (Exception ex) {
      return null;
    }
  }

  private void populateAuthorTaskStatus(Article article, ArticleResponse response) {
    if (article.getId() == null) {
      return;
    }
    extractionRepository.findFirstByArticleIdOrderByExtractedAtDesc(article.getId())
        .map(extraction -> extraction.getStatus().name())
        .ifPresent(response::setAuthorTaskStatus);

    if (response.getAuthorTaskStatus() != null) {
      return;
    }
    taskRepository.findTopByArticleIdAndTaskTypeOrderByCreatedAtDesc(
            article.getId(),
            ai.journa.prcontrol.domain.EnrichmentTaskType.EXTRACT_AUTHOR
        )
        .ifPresent(task -> response.setAuthorTaskStatus(task.getStatus().name()));
  }
}
