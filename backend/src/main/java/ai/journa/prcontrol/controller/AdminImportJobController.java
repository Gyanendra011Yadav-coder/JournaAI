package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.ImportJob;
import ai.journa.prcontrol.domain.ImportJobRow;
import ai.journa.prcontrol.dto.ImportJobResponse;
import ai.journa.prcontrol.dto.ImportJobRowResponse;
import ai.journa.prcontrol.repository.ImportJobRepository;
import ai.journa.prcontrol.repository.ImportJobRowRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/import-jobs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImportJobController {
  private final ImportJobRepository importJobRepository;
  private final ImportJobRowRepository importJobRowRepository;

  public AdminImportJobController(ImportJobRepository importJobRepository,
                                  ImportJobRowRepository importJobRowRepository) {
    this.importJobRepository = importJobRepository;
    this.importJobRowRepository = importJobRowRepository;
  }

  @GetMapping("/{id}")
  public ImportJobResponse getJob(@PathVariable Long id) {
    ImportJob job = importJobRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Import job not found"));
    List<ImportJobRow> rows = importJobRowRepository.findByJobId(id);
    return toResponse(job, rows);
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
    return response;
  }
}
