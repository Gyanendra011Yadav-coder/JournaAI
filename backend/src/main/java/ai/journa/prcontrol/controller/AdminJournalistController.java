package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.*;
import ai.journa.prcontrol.dto.ImportJobResponse;
import ai.journa.prcontrol.dto.ImportJobRowResponse;
import ai.journa.prcontrol.dto.JournalistArticleSummary;
import ai.journa.prcontrol.dto.JournalistContactResponse;
import ai.journa.prcontrol.dto.JournalistResponse;
import ai.journa.prcontrol.dto.JournalistUpdateRequest;
import ai.journa.prcontrol.repository.ArticleJournalistRepository;
import ai.journa.prcontrol.repository.ImportJobRowRepository;
import ai.journa.prcontrol.service.CsvImportService;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.JournalistService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/journalists")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJournalistController {
  private final JournalistService journalistService;
  private final ArticleJournalistRepository articleJournalistRepository;
  private final CsvImportService csvImportService;
  private final CurrentUserService currentUserService;
  private final ImportJobRowRepository importJobRowRepository;

  public AdminJournalistController(JournalistService journalistService,
                                   ArticleJournalistRepository articleJournalistRepository,
                                   CsvImportService csvImportService,
                                   CurrentUserService currentUserService,
                                   ImportJobRowRepository importJobRowRepository) {
    this.journalistService = journalistService;
    this.articleJournalistRepository = articleJournalistRepository;
    this.csvImportService = csvImportService;
    this.currentUserService = currentUserService;
    this.importJobRowRepository = importJobRowRepository;
  }

  @GetMapping("/incomplete")
  public List<JournalistResponse> getIncomplete(@RequestParam(required = false) String missing,
                                                @RequestParam(required = false) String q) {
    List<Journalist> journalists = journalistService.findIncomplete(missing, q);
    return journalists.stream().map(this::toResponse).toList();
  }

  @PostMapping("/{id}/verify")
  public JournalistResponse verify(@PathVariable Long id,
                                   @RequestParam(defaultValue = "VERIFIED") JournalistVerificationStatus status,
                                   @RequestParam(defaultValue = "100") int completenessScore) {
    Journalist journalist = journalistService.getJournalist(id);
    Journalist updated = journalistService.updateVerification(journalist, status, completenessScore);
    return toResponse(updated);
  }

  @PostMapping("/{id}/merge")
  public void merge(@PathVariable Long id, @RequestParam Long targetId) {
    Journalist source = journalistService.getJournalist(id);
    Journalist target = journalistService.getJournalist(targetId);
    journalistService.mergeJournalists(source, target);
  }

  @PutMapping("/{id}")
  public JournalistResponse update(@PathVariable Long id, @RequestBody JournalistUpdateRequest request) {
    Journalist journalist = journalistService.getJournalist(id);
    Journalist updated = journalistService.updateDetails(journalist, request);
    return toResponse(updated);
  }

  @PostMapping("/import-csv")
  public ImportJobResponse importCsv(@RequestParam("file") MultipartFile file,
                                     @RequestParam(defaultValue = "false") boolean dryRun) {
    ImportJob job = csvImportService.importCsv(file, dryRun, currentUserService.requireCurrentUser());
    return toResponse(job, importJobRowRepository.findByJobId(job.getId()));
  }

  private JournalistResponse toResponse(Journalist journalist) {
    JournalistResponse response = new JournalistResponse();
    response.setId(journalist.getId());
    response.setFullName(journalist.getFullName());
    response.setName(journalist.getFullName());
    response.setPublicationName(journalist.getPublicationName());
    response.setPublicationDomain(journalist.getPublicationDomain());
    response.setDesignation(journalist.getDesignation());
    response.setLinkedin(journalist.getLinkedin());
    response.setBeats(journalist.getBeats() != null ? Arrays.asList(journalist.getBeats()) : List.of());
    response.setCountry(journalist.getCountry());
    response.setCity(journalist.getCity());
    response.setJourneySummary(journalist.getJourneySummary());
    response.setVerificationStatus(journalist.getVerificationStatus().name());
    response.setCompletenessScore(journalist.getCompletenessScore());
    response.setArticles(articleJournalistRepository.findByJournalistId(journalist.getId()).stream().map(link -> {
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

  private ImportJobResponse toResponse(ImportJob job, List<ImportJobRow> rows) {
    ImportJobResponse response = new ImportJobResponse();
    response.setId(job.getId());
    response.setStatus(job.getStatus().name());
    response.setDryRun(job.isDryRun());
    response.setCreatedAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
    response.setSourceName(job.getSourceName());
    response.setSummaryJsonb(job.getSummaryJsonb());
    response.setRows(rows.stream().map(this::toRowResponse).toList());
    return response;
  }

  private ImportJobRowResponse toRowResponse(ImportJobRow row) {
    ImportJobRowResponse response = new ImportJobRowResponse();
    response.setId(row.getId());
    response.setRowNumber(row.getRowNumber());
    response.setStatus(row.getStatus().name());
    response.setMessage(row.getMessage());
    response.setJournalistId(row.getJournalist() != null ? row.getJournalist().getId() : null);
    response.setPayloadJsonb(row.getPayloadJsonb());
    return response;
  }
}
