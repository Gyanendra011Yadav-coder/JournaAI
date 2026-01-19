package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.ContactSourceType;
import ai.journa.prcontrol.domain.ContactVisibility;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.Role;
import ai.journa.prcontrol.domain.User;
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

  public JournalistContact addContact(Journalist journalist, String email, String phone, User actor) {
    if (journalist == null || journalist.getId() == null) {
      throw new IllegalStateException("Journalist not found");
    }
    String normalizedEmail = normalizeEmail(email);
    String normalizedPhone = normalizePhone(phone);
    if ((normalizedEmail == null || normalizedEmail.isBlank())
        && (normalizedPhone == null || normalizedPhone.isBlank())) {
      throw new IllegalStateException("Email or phone is required");
    }
    List<JournalistContact> existing = contactRepository.findByJournalistId(journalist.getId());
    boolean emailExists = false;
    boolean phoneExists = false;
    for (JournalistContact contact : existing) {
      if (!emailExists && normalizedEmail != null && contact.getEmail() != null
          && normalizedEmail.equalsIgnoreCase(contact.getEmail().trim())) {
        emailExists = true;
      }
      if (!phoneExists && normalizedPhone != null && contact.getPhone() != null
          && normalizedPhone.equals(normalizePhone(contact.getPhone()))) {
        phoneExists = true;
      }
    }
    String emailToSave = emailExists ? null : normalizedEmail;
    String phoneToSave = phoneExists ? null : normalizedPhone;
    if ((emailToSave == null || emailToSave.isBlank()) && (phoneToSave == null || phoneToSave.isBlank())) {
      return existing.isEmpty() ? null : existing.get(0);
    }
    JournalistContact contact = new JournalistContact();
    contact.setJournalist(journalist);
    contact.setEmail(emailToSave);
    contact.setPhone(phoneToSave);
    contact.setVisibility(ContactVisibility.PUBLIC);
    if (actor != null && actor.getRole() == Role.ADMIN) {
      contact.setSourceType(ContactSourceType.MANUAL);
    } else {
      contact.setSourceType(ContactSourceType.COMMUNITY_SHARED);
    }
    return contactRepository.save(contact);
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
      journalist.setBeats(normalizeArray(request.getBeats()));
    }
    if (request.getAliases() != null) {
      journalist.setAliases(normalizeArray(request.getAliases()));
    }
    if (request.getPublicationAliases() != null) {
      journalist.setPublicationAliases(normalizeArray(request.getPublicationAliases()));
    }
    if (request.getTopicKeywords() != null) {
      journalist.setTopicKeywords(normalizeArray(request.getTopicKeywords()));
    }
    if (request.getLanguages() != null) {
      journalist.setLanguages(normalizeArray(request.getLanguages()));
    }
    if (request.getCoverageRegions() != null) {
      journalist.setCoverageRegions(normalizeArray(request.getCoverageRegions()));
    }
    if (request.getOtherLinks() != null) {
      journalist.setOtherLinks(normalizeArray(request.getOtherLinks()));
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
    String normalizedSearchBy = searchBy == null ? "any" : searchBy.trim().toLowerCase(Locale.ROOT);
    if ("email".equals(normalizedSearchBy)) {
      return filterByEmail(journalists, needle);
    }
    if ("phone".equals(normalizedSearchBy)) {
      return filterByPhone(journalists, needle);
    }
    if ("any".equals(normalizedSearchBy)) {
      return journalists.stream()
          .filter(journalist -> matchesAny(journalist, needle))
          .toList();
    }
    return journalists.stream()
        .filter(journalist -> matchesSearch(journalist, needle, normalizedSearchBy))
        .toList();
  }

  private int calculateCompletenessScore(Journalist journalist) {
    if (journalist == null) {
      return 0;
    }
    int totalFields = 12;
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
    if (hasText(journalist.getAuthorPageUrl()) || hasText(journalist.getTwitter())
        || hasText(journalist.getLinkedin())
        || (journalist.getOtherLinks() != null && journalist.getOtherLinks().length > 0)) {
      filled++;
    }
    if (hasText(journalist.getCountry()) || hasText(journalist.getCity())
        || (journalist.getCoverageRegions() != null && journalist.getCoverageRegions().length > 0)) {
      filled++;
    }
    if (journalist.getAliases() != null && journalist.getAliases().length > 0) {
      filled++;
    }
    if (journalist.getTopicKeywords() != null && journalist.getTopicKeywords().length > 0) {
      filled++;
    }
    if (journalist.getLanguages() != null && journalist.getLanguages().length > 0) {
      filled++;
    }
    if (journalist.getId() != null) {
      List<JournalistContact> contacts = contactRepository.findByJournalistId(journalist.getId());
      if (contacts != null && !contacts.isEmpty()) {
        filled++;
      }
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
    if ("all".equals(normalized) || "any".equals(normalized)) {
      return journalistRepository.findAll();
    }
    if ("email".equals(normalized)) {
      return journalistRepository.findByVerificationStatus(JournalistVerificationStatus.UNVERIFIED);
    }
    if ("beats".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getBeats() == null || journalist.getBeats().length == 0)
          .toList();
    }
    if ("designation".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getDesignation() == null || journalist.getDesignation().isBlank())
          .toList();
    }
    if ("publication".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getPublicationName() == null
              || journalist.getPublicationName().isBlank())
          .toList();
    }
    if ("links".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> !hasText(journalist.getAuthorPageUrl())
              && !hasText(journalist.getTwitter())
              && !hasText(journalist.getLinkedin())
              && (journalist.getOtherLinks() == null || journalist.getOtherLinks().length == 0))
          .toList();
    }
    if ("bio".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> !hasText(journalist.getBioSummary())
              && !hasText(journalist.getJourneySummary()))
          .toList();
    }
    if ("location".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> !hasText(journalist.getCountry())
              && !hasText(journalist.getCity())
              && (journalist.getCoverageRegions() == null || journalist.getCoverageRegions().length == 0))
          .toList();
    }
    if ("aliases".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getAliases() == null || journalist.getAliases().length == 0)
          .toList();
    }
    if ("topics".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getTopicKeywords() == null
              || journalist.getTopicKeywords().length == 0)
          .toList();
    }
    if ("languages".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getLanguages() == null
              || journalist.getLanguages().length == 0)
          .toList();
    }
    if ("regions".equals(normalized)) {
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getCoverageRegions() == null
              || journalist.getCoverageRegions().length == 0)
          .toList();
    }
    if ("contacts".equals(normalized)) {
      List<Long> withContacts = contactRepository.findJournalistIdsWithContacts();
      if (withContacts.isEmpty()) {
        return journalistRepository.findAll();
      }
      Set<Long> idSet = new HashSet<>(withContacts);
      return journalistRepository.findAll().stream()
          .filter(journalist -> journalist.getId() != null && !idSet.contains(journalist.getId()))
          .toList();
    }
    return journalistRepository.findAll();
  }

  private boolean matchesSearch(Journalist journalist, String needle, String searchBy) {
    return switch (searchBy) {
      case "beat" -> matchesBeats(journalist, needle);
      case "alias" -> matchesAliases(journalist, needle);
      case "topic" -> matchesTopics(journalist, needle);
      case "language" -> matchesLanguages(journalist, needle);
      case "location" -> matchesRegions(journalist, needle);
      case "region" -> matchesRegions(journalist, needle);
      case "designation" -> contains(journalist.getDesignation(), needle);
      case "publication" -> contains(journalist.getPublicationName(), needle)
          || contains(journalist.getPublicationDomain(), needle)
          || matchesArray(journalist.getPublicationAliases(), needle);
      default -> contains(journalist.getFullName(), needle);
    };
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    String trimmed = email.trim().toLowerCase(Locale.ROOT);
    if (trimmed.isBlank()) {
      return null;
    }
    if (!trimmed.contains("@")) {
      return null;
    }
    return trimmed;
  }

  private String normalizePhone(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    boolean hasPlus = trimmed.startsWith("+") || trimmed.startsWith("00");
    String digits = trimmed.replaceAll("[^0-9]", "");
    if (trimmed.startsWith("00") && digits.length() > 2) {
      digits = digits.substring(2);
    }
    if (digits.length() < 8 || digits.length() > 15) {
      return null;
    }
    return hasPlus ? "+" + digits : digits;
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

  private List<Journalist> filterByPhone(List<Journalist> journalists, String needle) {
    List<Long> ids = contactRepository.findJournalistIdsByPhoneLike(needle);
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

  private boolean matchesAliases(Journalist journalist, String needle) {
    return matchesArray(journalist != null ? journalist.getAliases() : null, needle);
  }

  private boolean matchesTopics(Journalist journalist, String needle) {
    return matchesArray(journalist != null ? journalist.getTopicKeywords() : null, needle);
  }

  private boolean matchesLanguages(Journalist journalist, String needle) {
    return matchesArray(journalist != null ? journalist.getLanguages() : null, needle);
  }

  private boolean matchesRegions(Journalist journalist, String needle) {
    if (journalist == null) {
      return false;
    }
    if (contains(journalist.getCountry(), needle) || contains(journalist.getCity(), needle)) {
      return true;
    }
    return matchesArray(journalist.getCoverageRegions(), needle);
  }

  private boolean matchesAny(Journalist journalist, String needle) {
    if (journalist == null) {
      return false;
    }
    return contains(journalist.getFullName(), needle)
        || matchesAliases(journalist, needle)
        || contains(journalist.getPublicationName(), needle)
        || contains(journalist.getPublicationDomain(), needle)
        || matchesArray(journalist.getPublicationAliases(), needle)
        || contains(journalist.getDesignation(), needle)
        || matchesBeats(journalist, needle)
        || matchesTopics(journalist, needle)
        || matchesLanguages(journalist, needle)
        || matchesRegions(journalist, needle)
        || contains(journalist.getAuthorPageUrl(), needle)
        || contains(journalist.getTwitter(), needle)
        || contains(journalist.getLinkedin(), needle)
        || matchesArray(journalist.getOtherLinks(), needle);
  }

  private boolean matchesArray(String[] values, String needle) {
    if (values == null || needle == null || needle.isBlank()) {
      return false;
    }
    for (String value : values) {
      if (contains(value, needle)) {
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

  private String[] normalizeArray(List<String> values) {
    if (values == null) {
      return null;
    }
    String[] normalized = values.stream()
        .filter(item -> item != null && !item.isBlank())
        .map(item -> item.trim())
        .distinct()
        .toArray(String[]::new);
    return normalized.length > 0 ? normalized : null;
  }
}
