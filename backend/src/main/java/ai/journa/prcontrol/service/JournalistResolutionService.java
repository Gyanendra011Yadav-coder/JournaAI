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
    String domain = resolveDomain(article != null ? article.getSourceUrl() : null);
    Journalist journalist = null;
    if (domain != null) {
      journalist = journalistRepository.findByPublicationDomainIgnoreCaseAndFullNameIgnoreCase(domain, authorName)
          .orElse(null);
    }
    MatchMethod method = MatchMethod.EXACT;
    if (journalist == null) {
      journalist = new Journalist();
      journalist.setFullName(authorName.trim());
      journalist.setPublicationDomain(domain);
      journalist.setPublicationName(article != null ? article.getSourceName() : null);
      applyBeatTag(journalist, article);
      journalist = journalistRepository.save(journalist);
      method = MatchMethod.FUZZY;
    }
    applyBeatTag(journalist, article);
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

  private String resolveDomain(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    try {
      String host = URI.create(url).getHost();
      return host != null ? host.toLowerCase(Locale.ROOT) : null;
    } catch (Exception ex) {
      return null;
    }
  }
}
