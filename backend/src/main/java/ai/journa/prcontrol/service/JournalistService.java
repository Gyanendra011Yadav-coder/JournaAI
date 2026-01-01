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
import java.util.HashSet;
import java.util.Set;

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

  public List<Journalist> findIncomplete(String missingField, String query, String searchBy) {
    List<Journalist> base = resolveMissingFilter(missingField);
    return filterByQuery(base, query, searchBy);
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
    if (request.getTwitter() != null) {
      journalist.setTwitter(request.getTwitter().trim());
    }
    if (request.getAuthorPageUrl() != null) {
      journalist.setAuthorPageUrl(request.getAuthorPageUrl().trim());
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
    if (request.getBioSummary() != null) {
      journalist.setBioSummary(request.getBioSummary().trim());
    }
    if (request.getVerificationStatus() != null) {
      journalist.setVerificationStatus(
          JournalistVerificationStatus.valueOf(request.getVerificationStatus().trim().toUpperCase(Locale.ROOT))
      );
    }
    if (request.getCompletenessScore() != null) {
      journalist.setCompletenessScore(request.getCompletenessScore());
    } else {
      int computed = calculateCompletenessScore(journalist);
      if (computed > journalist.getCompletenessScore()) {
        journalist.setCompletenessScore(computed);
      }
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

  private List<Journalist> filterByQuery(List<Journalist> journalists, String query, String searchBy) {
    if (query == null || query.isBlank()) {
      return journalists;
    }
    String needle = query.trim().toLowerCase(Locale.ROOT);
    String normalizedSearchBy = searchBy == null ? "name" : searchBy.trim().toLowerCase(Locale.ROOT);
    if ("email".equals(normalizedSearchBy)) {
      return filterByEmail(journalists, needle);
    }
    return journalists.stream()
        .filter(journalist -> matchesSearch(journalist, needle, normalizedSearchBy))
        .toList();
  }

  private int calculateCompletenessScore(Journalist journalist) {
    if (journalist == null) {
      return 0;
    }
    int totalFields = 7;
    int filled = 0;
    if (hasText(journalist.getFullName())) {
      filled++;
    }
    if (hasText(journalist.getPublicationName())) {
      filled++;
    }
    if (hasText(journalist.getPublicationDomain())) {
      filled++;
    }
    if (hasText(journalist.getDesignation())) {
      filled++;
    }
    if (journalist.getBeats() != null && journalist.getBeats().length > 0) {
      filled++;
    }
    if (hasText(journalist.getBioSummary()) || hasText(journalist.getJourneySummary())) {
      filled++;
    }
    if (hasText(journalist.getAuthorPageUrl()) || hasText(journalist.getTwitter()) || hasText(journalist.getLinkedin())) {
      filled++;
    }
    return (int) Math.round((filled * 100.0) / totalFields);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private List<Journalist> resolveMissingFilter(String missingField) {
    if (missingField == null || missingField.isBlank()) {
      return journalistRepository.findAll();
    }
    String normalized = missingField.trim().toLowerCase(Locale.ROOT);
    if ("email".equals(normalized)) {
      return journalistRepository.findByVerificationStatus(JournalistVerificationStatus.UNVERIFIED);
    }
    if ("beats".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getBeats() == null || journalist.getBeats().length == 0)
          .toList();
    }
    if ("publication".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getPublicationName() == null
              || journalist.getPublicationName().isBlank())
          .toList();
    }
    return journalistRepository.findAll();
  }

  private boolean matchesSearch(Journalist journalist, String needle, String searchBy) {
    return switch (searchBy) {
      case "beat" -> matchesBeats(journalist, needle);
      case "publication" -> contains(journalist.getPublicationName(), needle)
          || contains(journalist.getPublicationDomain(), needle);
      default -> contains(journalist.getFullName(), needle);
    };
  }

  private List<Journalist> filterByEmail(List<Journalist> journalists, String needle) {
    List<Long> ids = contactRepository.findJournalistIdsByEmailLike(needle);
    if (ids.isEmpty()) {
      return List.of();
    }
    if (journalists.isEmpty()) {
      return List.of();
    }
    if (journalists.size() == journalistRepository.count()) {
      return journalistRepository.findAllById(ids);
    }
    Set<Long> idSet = new HashSet<>(ids);
    return journalists.stream()
        .filter(journalist -> journalist != null && journalist.getId() != null && idSet.contains(journalist.getId()))
        .toList();
  }

  private boolean matchesBeats(Journalist journalist, String needle) {
    if (journalist == null || journalist.getBeats() == null) {
      return false;
    }
    for (String beat : journalist.getBeats()) {
      if (contains(beat, needle)) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(String value, String needle) {
    if (value == null) {
      return false;
    }
    return value.toLowerCase(Locale.ROOT).contains(needle);
  }
}
