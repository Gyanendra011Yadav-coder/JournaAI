package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.ContactSourceType;
import ai.journa.prcontrol.domain.ContactVisibility;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.JournalistEnrichmentReview;
import ai.journa.prcontrol.domain.JournalistEnrichmentReviewStatus;
import ai.journa.prcontrol.repository.JournalistContactRepository;
import ai.journa.prcontrol.repository.JournalistEnrichmentReviewRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class JournalistEnrichmentReviewService {
  private final JournalistEnrichmentReviewRepository reviewRepository;
  private final JournalistRepository journalistRepository;
  private final JournalistContactRepository contactRepository;
  private final ObjectMapper objectMapper;

  public JournalistEnrichmentReviewService(JournalistEnrichmentReviewRepository reviewRepository,
                                           JournalistRepository journalistRepository,
                                           JournalistContactRepository contactRepository,
                                           ObjectMapper objectMapper) {
    this.reviewRepository = reviewRepository;
    this.journalistRepository = journalistRepository;
    this.contactRepository = contactRepository;
    this.objectMapper = objectMapper;
  }

  public Optional<JournalistEnrichmentReview> createReview(Journalist journalist, JsonNode proposedPayload) {
    if (journalist == null || proposedPayload == null) {
      return Optional.empty();
    }
    JsonNode proposedProfile = extractProposedProfile(proposedPayload);
    Map<String, String> current = buildCurrentSnapshot(journalist);
    Map<String, String> proposed = buildProposedSnapshot(proposedProfile, proposedPayload);
    Map<String, Map<String, String>> diff = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : proposed.entrySet()) {
      String field = entry.getKey();
      String proposedValue = entry.getValue();
      if (proposedValue == null || proposedValue.isBlank()) {
        continue;
      }
      String currentValue = current.get(field);
      if (currentValue != null && isTruncatedVariant(currentValue, proposedValue)) {
        continue;
      }
      if (currentValue == null || currentValue.isBlank()) {
        diff.put(field, Map.of("current", "", "proposed", proposedValue));
      } else if (!equalsNormalized(currentValue, proposedValue)) {
        diff.put(field, Map.of("current", currentValue, "proposed", proposedValue));
      }
    }
    if (diff.isEmpty()) {
      return Optional.empty();
    }
    JournalistEnrichmentReview review = new JournalistEnrichmentReview();
    review.setJournalist(journalist);
    review.setStatus(JournalistEnrichmentReviewStatus.PENDING);
    review.setCurrentJsonb(writeJson(current));
    review.setProposedJsonb(writeJson(proposedPayload));
    review.setDiffJsonb(writeJson(diff));
    review.setCreatedAt(Instant.now());
    review.setUpdatedAt(Instant.now());
    return Optional.of(reviewRepository.save(review));
  }

  public Optional<JournalistEnrichmentReview> applyReview(Long reviewId) {
    JournalistEnrichmentReview review = reviewRepository.findById(reviewId).orElse(null);
    if (review == null || review.getStatus() != JournalistEnrichmentReviewStatus.PENDING) {
      return Optional.empty();
    }
    Journalist journalist = journalistRepository.findById(review.getJournalist().getId()).orElse(null);
    if (journalist == null) {
      return Optional.empty();
    }
    JsonNode proposedPayload = readJson(review.getProposedJsonb());
    if (proposedPayload != null && proposedPayload.isObject()) {
      applySnapshot(journalist, proposedPayload);
      applyContactProposals(journalist, proposedPayload);
      int computedScore = calculateCompletenessScore(journalist);
      if (computedScore > journalist.getCompletenessScore()) {
        journalist.setCompletenessScore(computedScore);
      }
      journalist.setUpdatedAt(Instant.now());
      journalist.setLastUpdatedAt(Instant.now());
      journalistRepository.save(journalist);
    }
    review.setStatus(JournalistEnrichmentReviewStatus.APPLIED);
    review.setUpdatedAt(Instant.now());
    return Optional.of(reviewRepository.save(review));
  }

  public Optional<JournalistEnrichmentReview> dismissReview(Long reviewId) {
    JournalistEnrichmentReview review = reviewRepository.findById(reviewId).orElse(null);
    if (review == null || review.getStatus() != JournalistEnrichmentReviewStatus.PENDING) {
      return Optional.empty();
    }
    review.setStatus(JournalistEnrichmentReviewStatus.DISMISSED);
    review.setUpdatedAt(Instant.now());
    return Optional.of(reviewRepository.save(review));
  }

  private Map<String, String> buildCurrentSnapshot(Journalist journalist) {
    Map<String, String> snapshot = new LinkedHashMap<>();
    snapshot.put("full_name", safe(journalist.getFullName()));
    snapshot.put("publication_name", safe(journalist.getPublicationName()));
    snapshot.put("publication_domain", safe(journalist.getPublicationDomain()));
    snapshot.put("designation", safe(journalist.getDesignation()));
    snapshot.put("country", safe(journalist.getCountry()));
    snapshot.put("city", safe(journalist.getCity()));
    snapshot.put("author_page_url", safe(journalist.getAuthorPageUrl()));
    snapshot.put("twitter", safe(journalist.getTwitter()));
    snapshot.put("linkedin", safe(journalist.getLinkedin()));
    snapshot.put("bio_summary", safe(journalist.getBioSummary()));
    snapshot.put("journey_summary", safe(journalist.getJourneySummary()));
    if (journalist.getBeats() != null) {
      snapshot.put("beats", String.join(", ", journalist.getBeats()));
    }
    if (journalist.getAliases() != null) {
      snapshot.put("aliases", String.join(", ", journalist.getAliases()));
    }
    if (journalist.getPublicationAliases() != null) {
      snapshot.put("publication_aliases", String.join(", ", journalist.getPublicationAliases()));
    }
    if (journalist.getTopicKeywords() != null) {
      snapshot.put("topic_keywords", String.join(", ", journalist.getTopicKeywords()));
    }
    if (journalist.getLanguages() != null) {
      snapshot.put("languages", String.join(", ", journalist.getLanguages()));
    }
    if (journalist.getCoverageRegions() != null) {
      snapshot.put("coverage_regions", String.join(", ", journalist.getCoverageRegions()));
    }
    if (journalist.getOtherLinks() != null) {
      snapshot.put("other_links", String.join(", ", journalist.getOtherLinks()));
    }
    if (journalist.getId() != null) {
      List<JournalistContact> contacts = contactRepository.findByJournalistId(journalist.getId());
      Set<String> emails = new LinkedHashSet<>();
      Set<String> phones = new LinkedHashSet<>();
      for (JournalistContact contact : contacts) {
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
          emails.add(contact.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (contact.getPhone() != null && !contact.getPhone().isBlank()) {
          String normalized = normalizePhone(contact.getPhone());
          if (!normalized.isBlank()) {
            phones.add(normalized);
          }
        }
      }
      if (!emails.isEmpty()) {
        snapshot.put("contact_emails", String.join(", ", emails));
      }
      if (!phones.isEmpty()) {
        snapshot.put("contact_phones", String.join(", ", phones));
      }
    }
    return snapshot;
  }

  private Map<String, String> buildProposedSnapshot(JsonNode proposedProfile, JsonNode proposedPayload) {
    if (proposedProfile == null || proposedProfile.isMissingNode()) {
      proposedProfile = objectMapper.createObjectNode();
    }
    Map<String, String> snapshot = new LinkedHashMap<>();
    snapshot.put("full_name", text(proposedProfile, "full_name"));
    snapshot.put("publication_name", text(proposedProfile, "publication_name"));
    snapshot.put("publication_domain", text(proposedProfile, "publication_domain"));
    snapshot.put("designation", text(proposedProfile, "designation"));
    snapshot.put("bio_summary", text(proposedProfile, "bio_summary"));
    snapshot.put("journey_summary", text(proposedProfile, "journey_summary"));

    JsonNode location = proposedProfile.path("location");
    snapshot.put("country", text(location, "country"));
    snapshot.put("city", text(location, "city"));

    JsonNode links = proposedProfile.path("public_links");
    snapshot.put("author_page_url", text(links, "author_page"));
    snapshot.put("twitter", text(links, "twitter"));
    snapshot.put("linkedin", text(links, "linkedin"));

    JsonNode beats = proposedProfile.path("beats");
    if (beats.isArray()) {
      StringBuilder builder = new StringBuilder();
      for (JsonNode node : beats) {
        String beat = node.asText(null);
        if (beat == null || beat.isBlank()) {
          continue;
        }
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append(beat.trim());
      }
      if (builder.length() > 0) {
        snapshot.put("beats", builder.toString());
      }
    }
    String aliases = joinArray(proposedProfile.path("aliases"));
    if (aliases != null) {
      snapshot.put("aliases", aliases);
    }
    String publicationAliases = joinArray(proposedProfile.path("publication_aliases"));
    if (publicationAliases != null) {
      snapshot.put("publication_aliases", publicationAliases);
    }
    String topicKeywords = joinArray(proposedProfile.path("topic_keywords"));
    if (topicKeywords != null) {
      snapshot.put("topic_keywords", topicKeywords);
    }
    String languages = joinArray(proposedProfile.path("languages"));
    if (languages != null) {
      snapshot.put("languages", languages);
    }
    String coverageRegions = joinArray(proposedProfile.path("coverage_regions"));
    if (coverageRegions != null) {
      snapshot.put("coverage_regions", coverageRegions);
    }
    String otherLinks = joinArray(links.path("other"));
    if (otherLinks != null) {
      snapshot.put("other_links", otherLinks);
    }
    ContactCandidates contacts = extractContactCandidates(proposedPayload);
    if (!contacts.emails().isEmpty()) {
      snapshot.put("contact_emails", String.join(", ", contacts.emails()));
    }
    if (!contacts.inferredEmails().isEmpty()) {
      snapshot.put("contact_emails_inferred", String.join(", ", contacts.inferredEmails()));
    }
    if (!contacts.phones().isEmpty()) {
      snapshot.put("contact_phones", String.join(", ", contacts.phones()));
    }
    return snapshot;
  }

  private void applySnapshot(Journalist journalist, JsonNode snapshot) {
    if (journalist == null || snapshot == null || !snapshot.isObject()) {
      return;
    }
    JsonNode source = snapshot.has("proposed_profile")
        ? snapshot.path("proposed_profile")
        : snapshot;
    if (!source.isObject()) {
      source = snapshot;
    }
    setIfPresent(journalist::setFullName, source, "full_name");
    setIfPresent(journalist::setPublicationName, source, "publication_name");
    setIfPresent(journalist::setPublicationDomain, source, "publication_domain");
    setIfPresent(journalist::setDesignation, source, "designation");
    setIfPresent(journalist::setBioSummary, source, "bio_summary");
    setIfPresent(journalist::setJourneySummary, source, "journey_summary");

    JsonNode location = source.path("location");
    if (location.isObject()) {
      setIfPresent(journalist::setCountry, location, "country");
      setIfPresent(journalist::setCity, location, "city");
    } else {
      setIfPresent(journalist::setCountry, source, "country");
      setIfPresent(journalist::setCity, source, "city");
    }

    JsonNode links = source.path("public_links");
    if (links.isObject()) {
      setIfPresent(journalist::setAuthorPageUrl, links, "author_page");
      setIfPresent(journalist::setTwitter, links, "twitter");
      setIfPresent(journalist::setLinkedin, links, "linkedin");
    } else {
      setIfPresent(journalist::setAuthorPageUrl, source, "author_page_url");
      setIfPresent(journalist::setTwitter, source, "twitter");
      setIfPresent(journalist::setLinkedin, source, "linkedin");
    }

    JsonNode beatsNode = source.path("beats");
    if (beatsNode.isArray()) {
      StringBuilder builder = new StringBuilder();
      for (JsonNode node : beatsNode) {
        String beat = node.asText(null);
        if (beat == null || beat.isBlank()) {
          continue;
        }
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append(beat.trim());
      }
      if (builder.length() > 0) {
        journalist.setBeats(builder.toString().split(",\\s*"));
      }
    } else {
      String beatsValue = text(source, "beats");
      if (beatsValue != null && !beatsValue.isBlank()) {
        String[] beats = beatsValue.split(",");
        for (int i = 0; i < beats.length; i++) {
          beats[i] = beats[i].trim();
        }
        journalist.setBeats(beats);
      }
    }
    journalist.setAliases(normalizeArray(source.path("aliases")));
    journalist.setPublicationAliases(normalizeArray(source.path("publication_aliases")));
    journalist.setTopicKeywords(normalizeArray(source.path("topic_keywords")));
    journalist.setLanguages(normalizeArray(source.path("languages")));
    journalist.setCoverageRegions(normalizeArray(source.path("coverage_regions")));
    JsonNode otherLinksNode = source.path("public_links").path("other");
    if (otherLinksNode.isMissingNode() || otherLinksNode.isNull()) {
      otherLinksNode = source.path("other_links");
    }
    journalist.setOtherLinks(normalizeArray(otherLinksNode));
  }

  private void applyContactProposals(Journalist journalist, JsonNode proposedPayload) {
    if (journalist == null || journalist.getId() == null || proposedPayload == null) {
      return;
    }
    ContactCandidates contacts = extractContactCandidates(proposedPayload);
    if (contacts.emails().isEmpty() && contacts.phones().isEmpty() && contacts.inferredEmails().isEmpty()) {
      return;
    }
    List<JournalistContact> existing = contactRepository.findByJournalistId(journalist.getId());
    Set<String> existingEmails = new LinkedHashSet<>();
    Set<String> existingPhones = new LinkedHashSet<>();
    for (JournalistContact contact : existing) {
      if (contact.getEmail() != null) {
        existingEmails.add(contact.getEmail().trim().toLowerCase(Locale.ROOT));
      }
      if (contact.getPhone() != null) {
        String normalized = normalizePhone(contact.getPhone());
        if (!normalized.isBlank()) {
          existingPhones.add(normalized);
        }
      }
    }
    List<JournalistContact> toCreate = new ArrayList<>();
    for (String email : contacts.emails()) {
      String normalized = normalizeEmail(email);
      if (normalized == null || normalized.isBlank() || existingEmails.contains(normalized)) {
        continue;
      }
      JournalistContact contact = new JournalistContact();
      contact.setJournalist(journalist);
      contact.setEmail(normalized);
      contact.setVisibility(ContactVisibility.PUBLIC);
      contact.setSourceType(ContactSourceType.PUBLIC_BIO);
      toCreate.add(contact);
      existingEmails.add(normalized);
    }
    for (String email : contacts.inferredEmails()) {
      String normalized = normalizeEmail(email);
      if (normalized == null || normalized.isBlank() || existingEmails.contains(normalized)) {
        continue;
      }
      JournalistContact contact = new JournalistContact();
      contact.setJournalist(journalist);
      contact.setEmail(normalized);
      contact.setVisibility(ContactVisibility.PUBLIC);
      contact.setSourceType(ContactSourceType.INFERRED);
      toCreate.add(contact);
      existingEmails.add(normalized);
    }
    for (String phone : contacts.phones()) {
      String normalized = normalizePhone(phone);
      if (normalized.isBlank() || existingPhones.contains(normalized)) {
        continue;
      }
      JournalistContact contact = new JournalistContact();
      contact.setJournalist(journalist);
      contact.setPhone(normalized);
      contact.setVisibility(ContactVisibility.PUBLIC);
      contact.setSourceType(ContactSourceType.PUBLIC_BIO);
      toCreate.add(contact);
      existingPhones.add(normalized);
    }
    if (!toCreate.isEmpty()) {
      contactRepository.saveAll(toCreate);
    }
  }

  private JsonNode extractProposedProfile(JsonNode payload) {
    if (payload == null) {
      return null;
    }
    if (payload.has("proposed_profile")) {
      return payload.path("proposed_profile");
    }
    return payload;
  }

  private ContactCandidates extractContactCandidates(JsonNode payload) {
    if (payload == null || payload.isMissingNode()) {
      return new ContactCandidates(Set.of(), Set.of(), Set.of());
    }
    JsonNode contactsNode = payload.path("contacts");
    if (!contactsNode.isObject()) {
      contactsNode = payload.path("proposed_contacts");
    }
    Set<String> emails = new LinkedHashSet<>();
    Set<String> inferredEmails = new LinkedHashSet<>();
    Set<String> phones = new LinkedHashSet<>();
    collectValues(contactsNode.path("emails"), emails);
    collectValues(contactsNode.path("inferred_emails"), inferredEmails);
    collectValues(contactsNode.path("phones"), phones);
    return new ContactCandidates(emails, phones, inferredEmails);
  }

  private void collectValues(JsonNode node, Set<String> target) {
    if (node == null || target == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        String value = item.asText(null);
        if (value != null && !value.isBlank()) {
          target.add(value.trim());
        }
      }
      return;
    }
    if (node.isTextual()) {
      String value = node.asText();
      if (!value.isBlank()) {
        target.add(value.trim());
      }
    }
  }

  private String joinArray(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      String value = node.asText();
      return value.isBlank() ? null : value.trim();
    }
    if (!node.isArray()) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    for (JsonNode item : node) {
      String value = item.asText(null);
      if (value == null || value.isBlank()) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append(", ");
      }
      builder.append(value.trim());
    }
    return builder.length() > 0 ? builder.toString() : null;
  }

  private String[] normalizeArray(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    List<String> values = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        String value = item.asText(null);
        if (value != null && !value.isBlank()) {
          values.add(value.trim());
        }
      }
    } else if (node.isTextual()) {
      String value = node.asText();
      if (!value.isBlank()) {
        values.addAll(List.of(value.split(",\\s*")));
      }
    }
    if (values.isEmpty()) {
      return null;
    }
    return values.stream()
        .filter(item -> item != null && !item.isBlank())
        .map(String::trim)
        .distinct()
        .toArray(String[]::new);
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    String trimmed = email.trim().toLowerCase(Locale.ROOT);
    return trimmed.contains("@") ? trimmed : null;
  }

  private String normalizePhone(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    boolean hasPlus = trimmed.startsWith("+") || trimmed.startsWith("00");
    String digits = trimmed.replaceAll("[^0-9]", "");
    if (trimmed.startsWith("00") && digits.length() > 2) {
      digits = digits.substring(2);
    }
    if (digits.length() < 8 || digits.length() > 15) {
      return "";
    }
    return hasPlus ? "+" + digits : digits;
  }

  private void setIfPresent(java.util.function.Consumer<String> setter, JsonNode node, String field) {
    String value = text(node, field);
    if (value != null && !value.isBlank()) {
      setter.accept(value);
    }
  }

  private String text(JsonNode node, String field) {
    if (node == null || node.isMissingNode()) {
      return null;
    }
    String value = node.path(field).asText(null);
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
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

  private JsonNode readJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      return null;
    }
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean equalsNormalized(String left, String right) {
    if (left == null || right == null) {
      return false;
    }
    return left.trim().equalsIgnoreCase(right.trim());
  }

  private boolean isTruncatedVariant(String current, String proposed) {
    if (current == null || proposed == null) {
      return false;
    }
    String currentValue = current.trim();
    String proposedValue = proposed.trim();
    if (proposedValue.isBlank() || currentValue.isBlank()) {
      return false;
    }
    if (proposedValue.length() >= currentValue.length()) {
      return false;
    }
    String normalizedCurrent = currentValue.toLowerCase(Locale.ROOT);
    String normalizedProposed = proposedValue.toLowerCase(Locale.ROOT);
    if (normalizedCurrent.startsWith(normalizedProposed) && currentValue.length() - proposedValue.length() >= 3) {
      return true;
    }
    return normalizedCurrent.contains(normalizedProposed)
        && normalizedProposed.length() < Math.max(4, normalizedCurrent.length() * 0.7);
  }

  private record ContactCandidates(Set<String> emails, Set<String> phones, Set<String> inferredEmails) {
  }
}
