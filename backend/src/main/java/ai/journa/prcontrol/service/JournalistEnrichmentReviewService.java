package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistEnrichmentReview;
import ai.journa.prcontrol.domain.JournalistEnrichmentReviewStatus;
import ai.journa.prcontrol.repository.JournalistEnrichmentReviewRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class JournalistEnrichmentReviewService {
  private final JournalistEnrichmentReviewRepository reviewRepository;
  private final JournalistRepository journalistRepository;
  private final ObjectMapper objectMapper;

  public JournalistEnrichmentReviewService(JournalistEnrichmentReviewRepository reviewRepository,
                                           JournalistRepository journalistRepository,
                                           ObjectMapper objectMapper) {
    this.reviewRepository = reviewRepository;
    this.journalistRepository = journalistRepository;
    this.objectMapper = objectMapper;
  }

  public Optional<JournalistEnrichmentReview> createReview(Journalist journalist, JsonNode proposedProfile) {
    if (journalist == null || proposedProfile == null) {
      return Optional.empty();
    }
    Map<String, String> current = buildCurrentSnapshot(journalist);
    Map<String, String> proposed = buildProposedSnapshot(proposedProfile);
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
    review.setProposedJsonb(writeJson(proposedProfile));
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
    JsonNode proposed = readJson(review.getProposedJsonb());
    if (proposed != null && proposed.isObject()) {
      applySnapshot(journalist, proposed);
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
    if (journalist.getBeats() != null) {
      snapshot.put("beats", String.join(", ", journalist.getBeats()));
    }
    return snapshot;
  }

  private Map<String, String> buildProposedSnapshot(JsonNode proposedProfile) {
    Map<String, String> snapshot = new LinkedHashMap<>();
    snapshot.put("full_name", text(proposedProfile, "full_name"));
    snapshot.put("publication_name", text(proposedProfile, "publication_name"));
    snapshot.put("publication_domain", text(proposedProfile, "publication_domain"));
    snapshot.put("designation", text(proposedProfile, "designation"));
    snapshot.put("bio_summary", text(proposedProfile, "bio_summary"));

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
}
