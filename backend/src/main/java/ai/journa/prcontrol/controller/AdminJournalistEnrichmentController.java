package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.JournalistEnrichmentReview;
import ai.journa.prcontrol.domain.JournalistEnrichmentReviewStatus;
import ai.journa.prcontrol.dto.JournalistEnrichmentReviewResponse;
import ai.journa.prcontrol.repository.JournalistEnrichmentReviewRepository;
import ai.journa.prcontrol.service.JournalistEnrichmentReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/journalist-enrichment")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJournalistEnrichmentController {
  private final JournalistEnrichmentReviewRepository reviewRepository;
  private final JournalistEnrichmentReviewService reviewService;

  public AdminJournalistEnrichmentController(JournalistEnrichmentReviewRepository reviewRepository,
                                             JournalistEnrichmentReviewService reviewService) {
    this.reviewRepository = reviewRepository;
    this.reviewService = reviewService;
  }

  @GetMapping
  public List<JournalistEnrichmentReviewResponse> list(@RequestParam(defaultValue = "PENDING") JournalistEnrichmentReviewStatus status) {
    return reviewRepository.findByStatusOrderByCreatedAtDesc(status).stream()
        .map(this::toResponse)
        .toList();
  }

  @PostMapping("/{id}/apply")
  public JournalistEnrichmentReviewResponse apply(@PathVariable Long id) {
    JournalistEnrichmentReview review = reviewService.applyReview(id)
        .orElseThrow(() -> new IllegalStateException("Review not found or already processed"));
    return toResponse(review);
  }

  @PostMapping("/{id}/dismiss")
  public JournalistEnrichmentReviewResponse dismiss(@PathVariable Long id) {
    JournalistEnrichmentReview review = reviewService.dismissReview(id)
        .orElseThrow(() -> new IllegalStateException("Review not found or already processed"));
    return toResponse(review);
  }

  private JournalistEnrichmentReviewResponse toResponse(JournalistEnrichmentReview review) {
    JournalistEnrichmentReviewResponse response = new JournalistEnrichmentReviewResponse();
    response.setId(review.getId());
    response.setJournalistId(review.getJournalist() != null ? review.getJournalist().getId() : null);
    response.setJournalistName(review.getJournalist() != null ? review.getJournalist().getFullName() : null);
    response.setStatus(review.getStatus().name());
    response.setCurrentJsonb(review.getCurrentJsonb());
    response.setProposedJsonb(review.getProposedJsonb());
    response.setDiffJsonb(review.getDiffJsonb());
    response.setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
    return response;
  }
}
