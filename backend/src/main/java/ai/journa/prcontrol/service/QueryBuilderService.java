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
import java.util.stream.Collectors;

@Service
public class QueryBuilderService {
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

  private String joinTerms(List<String> terms) {
    if (terms == null || terms.isEmpty()) {
      return "";
    }
    List<String> sanitized = terms.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(value -> value.replaceAll("\\s+", " ").trim())
        .distinct()
        .collect(Collectors.toList());
    if (sanitized.isEmpty()) {
      return "";
    }
    if (sanitized.size() == 1) {
      return sanitized.get(0);
    }
    return sanitized.stream()
        .map(term -> "\"" + term.replace("\"", "") + "\"")
        .collect(Collectors.joining(" OR "));
  }

  public record QueryBundle(BeatQueryTemplate template, String beatQuery, String clientQuery) {}
}
