package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.JournalistVerificationStatus;
import ai.journa.prcontrol.dto.JournalistUpdateRequest;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.JournalistContactRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.List;

@Service
public class JournalistService {
  private final JournalistRepository journalistRepository;
  private final JournalistContactRepository contactRepository;
  private final ArticleJournalistRepository articleJournalistRepository;

  public JournalistService(JournalistRepository journalistRepository,
                           JournalistContactRepository contactRepository,
                           ArticleJournalistRepository articleJournalistRepository) {
    this.journalistRepository = journalistRepository;
    this.contactRepository = contactRepository;
    this.articleJournalistRepository = articleJournalistRepository;
  }

  public Journalist getJournalist(Long id) {
    return journalistRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Journalist not found"));
  }

  public List<JournalistContact> getContacts(Long journalistId) {
    return contactRepository.findByJournalistId(journalistId);
  }

  public List<Journalist> findIncomplete(String missingField, String query) {
    if ("email".equalsIgnoreCase(missingField)) {
      List<Journalist> candidates = journalistRepository.findByVerificationStatus(JournalistVerificationStatus.UNVERIFIED);
      return filterByQuery(candidates, query);
    }
    return filterByQuery(journalistRepository.findAll(), query);
  }

  public Journalist updateVerification(Journalist journalist, JournalistVerificationStatus status, int completenessScore) {
    journalist.setVerificationStatus(status);
    journalist.setCompletenessScore(completenessScore);
    journalist.setUpdatedAt(Instant.now());
    return journalistRepository.save(journalist);
  }

  public Journalist updateDetails(Journalist journalist, JournalistUpdateRequest request) {
    if (request.getFullName() != null) {
      journalist.setFullName(request.getFullName().trim());
    }
    if (request.getPublicationName() != null) {
      journalist.setPublicationName(request.getPublicationName().trim());
    }
    if (request.getPublicationDomain() != null) {
      journalist.setPublicationDomain(request.getPublicationDomain().trim());
    }
    if (request.getDesignation() != null) {
      journalist.setDesignation(request.getDesignation().trim());
    }
    if (request.getLinkedin() != null) {
      journalist.setLinkedin(request.getLinkedin().trim());
    }
    if (request.getBeats() != null) {
      String[] beats = request.getBeats().stream()
          .filter(item -> item != null && !item.isBlank())
          .map(item -> item.trim())
          .distinct()
          .toArray(String[]::new);
      journalist.setBeats(beats.length > 0 ? beats : null);
    }
    if (request.getCountry() != null) {
      journalist.setCountry(request.getCountry().trim());
    }
    if (request.getCity() != null) {
      journalist.setCity(request.getCity().trim());
    }
    if (request.getJourneySummary() != null) {
      journalist.setJourneySummary(request.getJourneySummary().trim());
    }
    if (request.getVerificationStatus() != null) {
      journalist.setVerificationStatus(
          JournalistVerificationStatus.valueOf(request.getVerificationStatus().trim().toUpperCase(Locale.ROOT))
      );
    }
    if (request.getCompletenessScore() != null) {
      journalist.setCompletenessScore(request.getCompletenessScore());
    }
    journalist.setUpdatedAt(Instant.now());
    return journalistRepository.save(journalist);
  }

  public void mergeJournalists(Journalist source, Journalist target) {
    articleJournalistRepository.findByJournalistId(source.getId()).forEach(link -> {
      Long articleId = link.getArticle() != null ? link.getArticle().getId() : null;
      if (articleId != null
          && articleJournalistRepository.findByArticleIdAndJournalistId(articleId, target.getId()).isPresent()) {
        articleJournalistRepository.delete(link);
        return;
      }
      link.setJournalist(target);
      articleJournalistRepository.save(link);
    });
    journalistRepository.delete(source);
  }

  private List<Journalist> filterByQuery(List<Journalist> journalists, String query) {
    if (query == null || query.isBlank()) {
      return journalists;
    }
    String needle = query.trim().toLowerCase(Locale.ROOT);
    return journalists.stream()
        .filter(journalist -> contains(journalist.getFullName(), needle)
            || contains(journalist.getPublicationName(), needle)
            || contains(journalist.getPublicationDomain(), needle))
        .toList();
  }

  private boolean contains(String value, String needle) {
    if (value == null) {
      return false;
    }
    return value.toLowerCase(Locale.ROOT).contains(needle);
  }
}
