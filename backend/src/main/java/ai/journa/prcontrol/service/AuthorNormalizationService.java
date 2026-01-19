package ai.journa.prcontrol.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthorNormalizationService {
  private static final Set<String> NON_PERSON_MARKERS = Set.of(
      "staff",
      "desk",
      "bureau",
      "team",
      "correspondent",
      "staff reporter",
      "news service",
      "news agency",
      "news desk",
      "tnn",
      "news network",
      "agencies",
      "pti",
      "ani",
      "reuters",
      "ap",
      "associated press",
      "press trust",
      "editorial",
      "newsroom",
      "ians",
      "toi",
      "et",
      "afp",
      "upi",
      "dpa"
  );
  private static final Set<String> ORG_WORDS = Set.of(
      "times",
      "news",
      "express",
      "post",
      "daily",
      "tribune",
      "chronicle",
      "mirror",
      "observer",
      "journal",
      "gazette",
      "telegraph",
      "channel",
      "tv",
      "media",
      "press",
      "agency"
  );

  public List<Candidate> normalizeCandidates(List<String> rawCandidates) {
    List<Candidate> results = new ArrayList<>();
    for (String raw : rawCandidates) {
      if (raw == null) {
        continue;
      }
      String trimmed = stripPrefixes(raw);
      for (String part : splitCandidates(trimmed)) {
        String cleaned = part.trim();
        if (cleaned.isBlank()) {
          continue;
        }
        boolean nonPerson = isNonPerson(cleaned);
        results.add(new Candidate(cleaned, nonPerson));
      }
    }
    return results;
  }

  public boolean isNonPerson(String value) {
    if (value == null || value.isBlank()) {
      return true;
    }
    String trimmed = value.trim();
    String lowerRaw = trimmed.toLowerCase(Locale.ROOT);
    if (lowerRaw.contains("http") || lowerRaw.contains("www.") || lowerRaw.contains(".com")
        || lowerRaw.contains(".in") || lowerRaw.contains(".org") || lowerRaw.contains("/")) {
      return true;
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("@")) {
      return true;
    }
    if (normalized.contains("staff")) {
      return true;
    }
    if (isShortAcronym(trimmed)) {
      return true;
    }
    if (containsOrgWord(trimmed)) {
      return true;
    }
    return NON_PERSON_MARKERS.stream().anyMatch(normalized::contains);
  }

  public boolean looksLikePersonName(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    String[] tokens = trimmed.split("\\s+");
    if (tokens.length < 2 || tokens.length > 4) {
      return false;
    }
    int normalTokens = 0;
    int initialTokens = 0;
    for (String token : tokens) {
      if (token == null || token.isBlank()) {
        continue;
      }
      if (containsOrgWord(token)) {
        return false;
      }
      String letters = token.replaceAll("[^\\p{L}]", "");
      if (letters.length() >= 2) {
        normalTokens++;
      } else if (letters.length() == 1 && token.endsWith(".")) {
        initialTokens++;
      } else {
        return false;
      }
    }
    return normalTokens >= 1 && (normalTokens + initialTokens) >= 2;
  }

  private boolean isShortAcronym(String value) {
    if (value == null) {
      return false;
    }
    String trimmed = value.trim();
    if (trimmed.contains(" ")) {
      return false;
    }
    if (trimmed.length() < 2 || trimmed.length() > 6) {
      return false;
    }
    boolean hasLetter = false;
    for (int i = 0; i < trimmed.length(); i++) {
      char ch = trimmed.charAt(i);
      if (!Character.isLetter(ch)) {
        return false;
      }
      hasLetter = true;
    }
    return hasLetter && trimmed.equals(trimmed.toUpperCase(Locale.ROOT));
  }

  private boolean containsOrgWord(String value) {
    if (value == null) {
      return false;
    }
    String[] tokens = value.toLowerCase(Locale.ROOT).split("\\s+");
    if (tokens.length == 0 || tokens.length > 4) {
      return false;
    }
    for (String token : tokens) {
      if (ORG_WORDS.contains(token)) {
        return true;
      }
    }
    return false;
  }

  private String stripPrefixes(String value) {
    String trimmed = value.trim();
    String lowered = trimmed.toLowerCase(Locale.ROOT);
    String[] prefixes = {"by ", "reported by ", "edited by ", "from ", "written by "};
    for (String prefix : prefixes) {
      if (lowered.startsWith(prefix)) {
        return trimmed.substring(prefix.length()).trim();
      }
    }
    return trimmed;
  }

  private List<String> splitCandidates(String value) {
    String normalized = value.replace("|", ",");
    normalized = normalized.replace(" and ", ",");
    String[] parts = normalized.split(",");
    List<String> results = new ArrayList<>();
    for (String part : parts) {
      results.add(part.trim());
    }
    return results;
  }

  public record Candidate(String name, boolean nonPerson) {
  }
}
