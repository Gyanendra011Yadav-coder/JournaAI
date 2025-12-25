package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Article;
import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.MatchMethod;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
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
    if (authorName == null || authorName.isBlank()) {
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
      journalist = journalistRepository.save(journalist);
      method = MatchMethod.FUZZY;
    }
    ArticleJournalist link = new ArticleJournalist();
    link.setArticle(article);
    link.setJournalist(journalist);
    link.setMatchConfidence(confidence);
    link.setMatchMethod(method);
    return Optional.of(articleJournalistRepository.save(link));
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
