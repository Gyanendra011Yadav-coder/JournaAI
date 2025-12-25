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
      "pti",
      "ani",
      "reuters",
      "ap",
      "associated press",
      "press trust",
      "editorial",
      "newsroom"
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
    String normalized = value.toLowerCase(Locale.ROOT);
    if (normalized.contains("staff")) {
      return true;
    }
    return NON_PERSON_MARKERS.stream().anyMatch(normalized::contains);
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
