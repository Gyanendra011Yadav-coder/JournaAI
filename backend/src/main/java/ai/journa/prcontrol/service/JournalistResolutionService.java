package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.MatchMethod;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Service
public class JournalistResolutionService {
  private final JournalistRepository journalistRepository;
  private final ArticleJournalistRepository articleJournalistRepository;

  public JournalistResolutionService(JournalistRepository journalistRepository,
                                     ArticleJournalistRepository articleJournalistRepository) {
    this.journalistRepository = journalistRepository;
    this.articleJournalistRepository = articleJournalistRepository;
  }

  public Optional<ArticleJournalist> resolve(Article article, String authorName, int confidence) {
    if (article == null || authorName == null || authorName.isBlank()) {
      return Optional.empty();
    }
    String cleanedAuthor = cleanName(authorName);
    String domain = resolveDomain(article);
    String sourceName = article.getSourceName();
    Journalist journalist = null;
    if (domain != null) {
      journalist = journalistRepository.findByPublicationDomainAndNameOrAlias(domain, cleanedAuthor)
          .orElse(null);
    }
    if (journalist == null && sourceName != null && !sourceName.isBlank()) {
      journalist = journalistRepository.findByPublicationNameAndNameOrAlias(sourceName, cleanedAuthor)
          .orElse(null);
    }
    MatchMethod method = MatchMethod.EXACT;
    if (journalist == null) {
      journalist = new Journalist();
      journalist.setFullName(cleanedAuthor);
      journalist.setPublicationDomain(domain);
      journalist.setPublicationName(sourceName);
      applySourceDetails(journalist, article, authorName, domain);
      applyBeatTag(journalist, article);
      journalist = journalistRepository.save(journalist);
      method = MatchMethod.FUZZY;
    } else {
      if (journalist.getFullName() == null
          || !journalist.getFullName().equalsIgnoreCase(cleanedAuthor)) {
        method = MatchMethod.FUZZY;
      }
      boolean updated = applySourceDetails(journalist, article, authorName, domain);
      applyBeatTag(journalist, article);
      if (updated) {
        journalistRepository.save(journalist);
      }
    }
    if (article.getId() != null && journalist.getId() != null) {
      Optional<ArticleJournalist> existing =
          articleJournalistRepository.findByArticleIdAndJournalistId(article.getId(), journalist.getId());
      if (existing.isPresent()) {
        ArticleJournalist link = existing.get();
        boolean updated = false;
        if (confidence > link.getMatchConfidence()) {
          link.setMatchConfidence(confidence);
          updated = true;
        }
        if (link.getMatchMethod() == null
            || (method == MatchMethod.EXACT && link.getMatchMethod() != MatchMethod.EXACT)) {
          link.setMatchMethod(method);
          updated = true;
        }
        return Optional.of(updated ? articleJournalistRepository.saveAndFlush(link) : link);
      }
    }
    ArticleJournalist link = new ArticleJournalist();
    link.setArticle(article);
    link.setJournalist(journalist);
    link.setMatchConfidence(confidence);
    link.setMatchMethod(method);
    try {
      return Optional.of(articleJournalistRepository.saveAndFlush(link));
    } catch (DataIntegrityViolationException ex) {
      if (article.getId() != null && journalist.getId() != null) {
        return articleJournalistRepository.findByArticleIdAndJournalistId(article.getId(), journalist.getId());
      }
      return Optional.empty();
    }
  }

  private void applyBeatTag(Journalist journalist, Article article) {
    if (journalist == null || article == null || article.getBeat() == null) {
      return;
    }
    String beatName = article.getBeat().getName();
    if (beatName == null || beatName.isBlank()) {
      return;
    }
    String normalized = beatName.trim();
    String[] beats = journalist.getBeats();
    if (beats != null) {
      for (String beat : beats) {
        if (beat != null && beat.equalsIgnoreCase(normalized)) {
          return;
        }
      }
      String[] updated = new String[beats.length + 1];
      System.arraycopy(beats, 0, updated, 0, beats.length);
      updated[beats.length] = normalized;
      journalist.setBeats(updated);
    } else {
      journalist.setBeats(new String[] { normalized });
    }
    if (journalist.getId() != null) {
      journalistRepository.save(journalist);
    }
  }

  private boolean applySourceDetails(Journalist journalist, Article article, String authorName, String domain) {
    if (journalist == null || article == null) {
      return false;
    }
    boolean updated = false;
    if ((journalist.getPublicationDomain() == null || journalist.getPublicationDomain().isBlank())
        && domain != null && !domain.isBlank()) {
      journalist.setPublicationDomain(domain);
      updated = true;
    }
    String sourceName = article.getSourceName();
    if ((journalist.getPublicationName() == null || journalist.getPublicationName().isBlank())
        && sourceName != null && !sourceName.isBlank()) {
      journalist.setPublicationName(sourceName);
      updated = true;
    }
    String[] aliasValues = mergeValues(journalist.getAliases(), buildAuthorAliases(authorName));
    if (aliasValues != journalist.getAliases()) {
      journalist.setAliases(aliasValues);
      updated = true;
    }
    String[] publicationAliases = mergeValues(journalist.getPublicationAliases(),
        buildPublicationAliases(sourceName));
    if (publicationAliases != journalist.getPublicationAliases()) {
      journalist.setPublicationAliases(publicationAliases);
      updated = true;
    }
    String[] topicKeywords = mergeValues(journalist.getTopicKeywords(), buildTopicKeywords(article));
    if (topicKeywords != journalist.getTopicKeywords()) {
      journalist.setTopicKeywords(topicKeywords);
      updated = true;
    }
    String[] languages = mergeValues(journalist.getLanguages(), normalizeToken(article.getLang()));
    if (languages != journalist.getLanguages()) {
      journalist.setLanguages(languages);
      updated = true;
    }
    String[] regions = mergeValues(journalist.getCoverageRegions(), normalizeToken(article.getSourceCountry()));
    if (regions != journalist.getCoverageRegions()) {
      journalist.setCoverageRegions(regions);
      updated = true;
    }
    return updated;
  }

  private String[] buildAuthorAliases(String authorName) {
    if (authorName == null || authorName.isBlank()) {
      return null;
    }
    String cleaned = cleanName(authorName);
    String stripped = cleaned.replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
    java.util.Set<String> values = new java.util.LinkedHashSet<>();
    if (!cleaned.isBlank()) {
      values.add(cleaned);
    }
    if (!stripped.isBlank()) {
      values.add(stripped);
    }
    String[] parts = stripped.split("\\s+");
    if (parts.length >= 2) {
      String first = parts[0];
      String last = parts[parts.length - 1];
      if (!first.isBlank() && !last.isBlank()) {
        values.add(last + ", " + first);
        values.add(first.substring(0, 1) + ". " + last);
      }
    }
    return values.isEmpty() ? null : values.toArray(new String[0]);
  }

  private String[] buildPublicationAliases(String publicationName) {
    if (publicationName == null || publicationName.isBlank()) {
      return null;
    }
    String cleaned = publicationName.trim();
    String noPrefix = cleaned.replaceFirst("(?i)^the\\s+", "").trim();
    String stripped = cleaned.replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
    java.util.Set<String> values = new java.util.LinkedHashSet<>();
    if (!noPrefix.isBlank() && !noPrefix.equalsIgnoreCase(cleaned)) {
      values.add(noPrefix);
    }
    if (!stripped.isBlank() && !stripped.equalsIgnoreCase(cleaned)) {
      values.add(stripped);
    }
    return values.isEmpty() ? null : values.toArray(new String[0]);
  }

  private String[] buildTopicKeywords(Article article) {
    if (article == null) {
      return null;
    }
    java.util.Set<String> values = new java.util.LinkedHashSet<>();
    if (article.getCategory() != null && !article.getCategory().isBlank()) {
      values.add(article.getCategory().trim());
    }
    if (article.getBeat() != null && article.getBeat().getName() != null) {
      String beatName = article.getBeat().getName().trim();
      if (!beatName.isBlank()) {
        values.add(beatName);
      }
    }
    return values.isEmpty() ? null : values.toArray(new String[0]);
  }

  private String[] mergeValues(String[] existing, String value) {
    if (value == null || value.isBlank()) {
      return existing;
    }
    return mergeValues(existing, new String[] { value });
  }

  private String[] mergeValues(String[] existing, String[] additions) {
    if (additions == null || additions.length == 0) {
      return existing;
    }
    java.util.Set<String> values = new java.util.LinkedHashSet<>();
    java.util.Set<String> normalized = new java.util.HashSet<>();
    if (existing != null) {
      for (String item : existing) {
        if (item != null && !item.isBlank()) {
          String trimmed = item.trim();
          values.add(trimmed);
          normalized.add(trimmed.toLowerCase(Locale.ROOT));
        }
      }
    }
    boolean changed = false;
    for (String item : additions) {
      if (item == null || item.isBlank()) {
        continue;
      }
      String trimmed = item.trim();
      String key = trimmed.toLowerCase(Locale.ROOT);
      if (!normalized.contains(key)) {
        values.add(trimmed);
        normalized.add(key);
        changed = true;
      }
    }
    if (values.isEmpty()) {
      return existing;
    }
    if (!changed) {
      return existing;
    }
    return values.toArray(new String[0]);
  }

  private String cleanName(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.replaceFirst("(?i)^(by|reviewed by|edited by|written by|reported by)\\s*[:\\-]?\\s*", "").trim();
  }

  private String normalizeToken(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String resolveDomain(Article article) {
    if (article == null) {
      return null;
    }
    String sourceUrl = article.getSourceUrl();
    String resolved = resolveDomainValue(sourceUrl);
    if (resolved != null && !resolved.isBlank()) {
      return resolved;
    }
    return resolveDomainValue(article.getUrl());
  }

  private String resolveDomainValue(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    try {
      String host = URI.create(url).getHost();
      if (host == null) {
        return null;
      }
      String normalized = host.toLowerCase(Locale.ROOT);
      return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    } catch (Exception ex) {
      return null;
    }
  }
}
