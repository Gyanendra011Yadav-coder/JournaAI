package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.ArticleJournalist;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.JournalistEnrichmentReviewStatus;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.JournalistArticleSummary;
import ai.journa.prcontrol.dto.JournalistContactRequest;
import ai.journa.prcontrol.dto.JournalistContactResponse;
import ai.journa.prcontrol.dto.JournalistResponse;
import ai.journa.prcontrol.dto.SuggestUpdateRequest;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.JournalistEnrichmentReviewRepository;
import ai.journa.prcontrol.service.AuditService;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.JournalistService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/journalists")
public class JournalistController {
  private final JournalistService journalistService;
  private final ArticleJournalistRepository articleJournalistRepository;
  private final CurrentUserService currentUserService;
  private final AuditService auditService;
  private final JournalistEnrichmentReviewRepository reviewRepository;

  public JournalistController(JournalistService journalistService,
                              ArticleJournalistRepository articleJournalistRepository,
                              CurrentUserService currentUserService,
                              AuditService auditService,
                              JournalistEnrichmentReviewRepository reviewRepository) {
    this.journalistService = journalistService;
    this.articleJournalistRepository = articleJournalistRepository;
    this.currentUserService = currentUserService;
    this.auditService = auditService;
    this.reviewRepository = reviewRepository;
  }

  @GetMapping("/{id}")
  public JournalistResponse getJournalist(@PathVariable Long id) {
    Journalist journalist = journalistService.getJournalist(id);
    List<ArticleJournalist> links = articleJournalistRepository.findByJournalistId(id);
    JournalistResponse response = toResponse(journalist, links);
    return response;
  }

  @PostMapping("/{id}/contacts")
  public JournalistContactResponse addContact(@PathVariable Long id, @RequestBody JournalistContactRequest request) {
    User actor = currentUserService.requireCurrentUser();
    Journalist journalist = journalistService.getJournalist(id);
    JournalistContact contact = journalistService.addContact(journalist, request.getEmail(), request.getPhone(), actor);
    if (contact == null) {
      throw new IllegalStateException("Contact already exists");
    }
    auditService.record(actor, "ADD_JOURNALIST_CONTACT", "journalist", request, String.valueOf(id));
    return toContactResponse(contact);
  }

  @PostMapping("/{id}/suggest-update")
  public void suggestUpdate(@PathVariable Long id, @RequestBody SuggestUpdateRequest request) {
    User actor = currentUserService.requireCurrentUser();
    auditService.record(actor, "SUGGEST_JOURNALIST_UPDATE", "journalist", request, String.valueOf(id));
  }

  private JournalistResponse toResponse(Journalist journalist, List<ArticleJournalist> links) {
    JournalistResponse response = new JournalistResponse();
    response.setId(journalist.getId());
    response.setFullName(journalist.getFullName());
    response.setName(journalist.getFullName());
    response.setPublicationName(journalist.getPublicationName());
    response.setPublicationDomain(journalist.getPublicationDomain());
    response.setDesignation(journalist.getDesignation());
    response.setLinkedin(journalist.getLinkedin());
    response.setTwitter(journalist.getTwitter());
    response.setAuthorPageUrl(journalist.getAuthorPageUrl());
    response.setBeats(journalist.getBeats() != null ? Arrays.asList(journalist.getBeats()) : List.of());
    response.setAliases(journalist.getAliases() != null ? Arrays.asList(journalist.getAliases()) : List.of());
    response.setPublicationAliases(journalist.getPublicationAliases() != null
        ? Arrays.asList(journalist.getPublicationAliases())
        : List.of());
    response.setTopicKeywords(journalist.getTopicKeywords() != null
        ? Arrays.asList(journalist.getTopicKeywords())
        : List.of());
    response.setLanguages(journalist.getLanguages() != null ? Arrays.asList(journalist.getLanguages()) : List.of());
    response.setCoverageRegions(journalist.getCoverageRegions() != null
        ? Arrays.asList(journalist.getCoverageRegions())
        : List.of());
    response.setOtherLinks(journalist.getOtherLinks() != null ? Arrays.asList(journalist.getOtherLinks()) : List.of());
    response.setCountry(journalist.getCountry());
    response.setCity(journalist.getCity());
    response.setJourneySummary(journalist.getJourneySummary());
    response.setBioSummary(journalist.getBioSummary());
    response.setVerificationStatus(journalist.getVerificationStatus().name());
    response.setCompletenessScore(journalist.getCompletenessScore());
    applyPendingReview(response, journalist);
    response.setArticles(links.stream().map(link -> {
      JournalistArticleSummary summary = new JournalistArticleSummary();
      summary.setArticleId(link.getArticle().getId());
      summary.setTitle(link.getArticle().getTitle());
      summary.setUrl(link.getArticle().getUrl());
      summary.setPublishedAtUtc(link.getArticle().getProviderPublishedAtUtc() != null
          ? link.getArticle().getProviderPublishedAtUtc().toString()
          : null);
      return summary;
    }).toList());
    List<JournalistContact> contacts = journalistService.getContacts(journalist.getId());
    response.setContacts(contacts.stream().map(this::toContactResponse).toList());
    return response;
  }

  private void applyPendingReview(JournalistResponse response, Journalist journalist) {
    if (journalist == null || journalist.getId() == null) {
      return;
    }
    reviewRepository.findTopByJournalistIdAndStatusOrderByCreatedAtDesc(
            journalist.getId(),
            JournalistEnrichmentReviewStatus.PENDING
        )
        .ifPresent(review -> {
          response.setPendingReviewId(review.getId());
          response.setPendingReviewStatus(review.getStatus().name());
          response.setPendingProfileJsonb(review.getProposedJsonb());
        });
  }

  private JournalistContactResponse toContactResponse(JournalistContact contact) {
    JournalistContactResponse response = new JournalistContactResponse();
    response.setId(contact.getId());
    response.setEmail(contact.getEmail());
    response.setPhone(contact.getPhone());
    response.setVisibility(contact.getVisibility().name());
    response.setSourceType(contact.getSourceType().name());
    response.setVerifiedAt(contact.getVerifiedAt() != null ? contact.getVerifiedAt().toString() : null);
    response.setVerifiedBy(contact.getVerifiedBy() != null ? contact.getVerifiedBy().getEmail() : null);
    return response;
  }
}
