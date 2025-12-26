package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryTemplate;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.domain.UserClient;
import ai.journa.prcontrol.domain.UserClientAlias;
import ai.journa.prcontrol.domain.UserKeyword;
import ai.journa.prcontrol.domain.UserKeywordKind;
import ai.journa.prcontrol.repository.BeatQueryTemplateRepository;
import ai.journa.prcontrol.repository.UserClientRepository;
import ai.journa.prcontrol.repository.UserKeywordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QueryBuilderService {
  private static final Pattern ADVANCED_QUERY_PATTERN =
      Pattern.compile("(?i)\\b(OR|AND|NOT)\\b|[\"()]");
  private final BeatQueryTemplateRepository templateRepository;
  private final UserKeywordRepository userKeywordRepository;
  private final UserClientRepository userClientRepository;

  public QueryBuilderService(BeatQueryTemplateRepository templateRepository,
                             UserKeywordRepository userKeywordRepository,
                             UserClientRepository userClientRepository) {
    this.templateRepository = templateRepository;
    this.userKeywordRepository = userKeywordRepository;
    this.userClientRepository = userClientRepository;
  }

  public QueryBundle buildSearchQueries(Beat beat, User user) {
    BeatQueryTemplate template = templateRepository.findByBeatId(beat.getId()).stream().findFirst()
        .orElseThrow(() -> new IllegalStateException("Beat query template not configured"));
    List<String> beatTerms = template.getBeatTerms();
    if (beatTerms == null || beatTerms.isEmpty()) {
      beatTerms = List.of(beat.getName());
    }

    List<String> clientTerms = new ArrayList<>();
    List<UserKeyword> keywordRows = new ArrayList<>();
    if (user != null) {
      List<UserClient> clients = userClientRepository.findByUser_Id(user.getId());
      for (UserClient client : clients) {
        if (client.getDisplayName() != null && !client.getDisplayName().isBlank()) {
          clientTerms.add(client.getDisplayName());
        }
        if (client.getShortName() != null && !client.getShortName().isBlank()) {
          clientTerms.add(client.getShortName());
        }
        for (UserClientAlias alias : client.getAliases()) {
          if (alias.getAlias() != null && !alias.getAlias().isBlank()) {
            clientTerms.add(alias.getAlias());
          }
        }
      }

      keywordRows = userKeywordRepository.findByUser_Id(user.getId());
      for (UserKeyword keyword : keywordRows) {
        if (keyword.getKind() == UserKeywordKind.CLIENT && keyword.getKeyword() != null) {
          clientTerms.add(keyword.getKeyword());
        }
      }
    }

    List<String> excludeTerms = keywordRows.stream()
        .filter(keyword -> keyword.getKind() == UserKeywordKind.EXCLUDE)
        .map(UserKeyword::getKeyword)
        .filter(value -> value != null && !value.isBlank())
        .toList();

    String beatClause = joinTerms(beatTerms);
    String excludeClause = joinTerms(excludeTerms);
    String clientClause = joinTerms(clientTerms);

    String beatQuery = beatClause;
    String clientQuery = clientClause.isBlank()
        ? beatClause
        : "(" + clientClause + ") AND (" + beatClause + ")";

    if (!excludeClause.isBlank()) {
      beatQuery = "(" + beatQuery + ") NOT (" + excludeClause + ")";
      clientQuery = "(" + clientQuery + ") NOT (" + excludeClause + ")";
    }

    return new QueryBundle(template, beatQuery, clientQuery);
  }

  public boolean hasClientTerms(User user) {
    if (user == null) {
      return false;
    }
    boolean hasClients = !userClientRepository.findByUser_Id(user.getId()).isEmpty();
    if (hasClients) {
      return true;
    }
    return !userKeywordRepository.findByUser_IdAndKind(user.getId(), UserKeywordKind.CLIENT).isEmpty();
  }

  private String joinTerms(List<String> terms) {
    if (terms == null || terms.isEmpty()) {
      return "";
    }
    List<String> sanitized = terms.stream()
        .filter(value -> value != null && !value.isBlank())
        .flatMap(value -> expandTerm(value).stream())
        .distinct()
        .collect(Collectors.toList());
    if (sanitized.isEmpty()) {
      return "";
    }
    if (sanitized.size() == 1) {
      return formatTerm(sanitized.get(0));
    }
    return sanitized.stream()
        .map(this::formatTerm)
        .collect(Collectors.joining(" OR "));
  }

  private List<String> expandTerm(String value) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isBlank()) {
      return List.of();
    }
    if (ADVANCED_QUERY_PATTERN.matcher(trimmed).find()) {
      return List.of(trimmed);
    }
    String normalized = trimmed.replaceAll("\\s+", " ").trim();
    String[] parts = normalized.split("[/&]");
    List<String> results = new ArrayList<>();
    for (String part : parts) {
      String cleaned = part.replaceAll("\\s+", " ").trim();
      if (!cleaned.isBlank()) {
        results.add(cleaned);
      }
    }
    return results;
  }

  private String formatTerm(String term) {
    if (term == null) {
      return "";
    }
    String trimmed = term.trim();
    if (trimmed.isBlank()) {
      return "";
    }
    if (ADVANCED_QUERY_PATTERN.matcher(trimmed).find()) {
      if (trimmed.contains("\"")) {
        return trimmed;
      }
      return normalizeAdvancedQuery(trimmed);
    }
    String cleaned = trimmed.replace("\"", "");
    if (cleaned.matches("^[A-Za-z0-9]+$")) {
      return cleaned;
    }
    return "\"" + cleaned + "\"";
  }

  private String normalizeAdvancedQuery(String query) {
    String spaced = query.replace("(", " ( ").replace(")", " ) ");
    String[] parts = spaced.trim().split("\\s+");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }
      String upper = part.toUpperCase(Locale.ROOT);
      String token;
      if ("(".equals(part) || ")".equals(part) || "AND".equals(upper) || "OR".equals(upper) || "NOT".equals(upper)) {
        token = upper.equals(part) ? part : upper;
      } else {
        token = "\"" + part.replace("\"", "") + "\"";
      }
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder.append(token);
    }
    return builder.toString();
  }

  public record QueryBundle(BeatQueryTemplate template, String beatQuery, String clientQuery) {}
}
