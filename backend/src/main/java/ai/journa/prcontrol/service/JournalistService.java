package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.JournalistVerificationStatus;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.JournalistContactRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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

  public List<Journalist> findIncomplete(String missingField) {
    if ("email".equalsIgnoreCase(missingField)) {
      return journalistRepository.findByVerificationStatus(JournalistVerificationStatus.UNVERIFIED);
    }
    return journalistRepository.findAll();
  }

  public Journalist updateVerification(Journalist journalist, JournalistVerificationStatus status, int completenessScore) {
    journalist.setVerificationStatus(status);
    journalist.setCompletenessScore(completenessScore);
    journalist.setUpdatedAt(Instant.now());
    return journalistRepository.save(journalist);
  }

  public void mergeJournalists(Journalist source, Journalist target) {
    articleJournalistRepository.findByJournalistId(source.getId()).forEach(link -> {
      link.setJournalist(target);
      articleJournalistRepository.save(link);
    });
    journalistRepository.delete(source);
  }
}
