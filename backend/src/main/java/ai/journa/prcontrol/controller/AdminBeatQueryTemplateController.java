package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.BeatQueryTemplate;
import ai.journa.prcontrol.dto.BeatQueryTemplateRequest;
import ai.journa.prcontrol.dto.BeatQueryTemplateResponse;
import ai.journa.prcontrol.service.BeatQueryTemplateService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/beat-query-templates")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBeatQueryTemplateController {
  private final BeatQueryTemplateService beatQueryTemplateService;

  public AdminBeatQueryTemplateController(BeatQueryTemplateService beatQueryTemplateService) {
    this.beatQueryTemplateService = beatQueryTemplateService;
  }

  @GetMapping
  public List<BeatQueryTemplateResponse> list(@RequestParam(required = false) Long beatId) {
    List<BeatQueryTemplate> templates = beatId != null
        ? beatQueryTemplateService.findByBeat(beatId)
        : beatQueryTemplateService.findAll();
    return templates.stream().map(this::toResponse).toList();
  }

  @PostMapping
  public BeatQueryTemplateResponse create(@Valid @RequestBody BeatQueryTemplateRequest request) {
    return toResponse(beatQueryTemplateService.create(request));
  }

  @PutMapping("/{id}")
  public BeatQueryTemplateResponse update(@PathVariable Long id, @Valid @RequestBody BeatQueryTemplateRequest request) {
    return toResponse(beatQueryTemplateService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    beatQueryTemplateService.delete(id);
  }

  private BeatQueryTemplateResponse toResponse(BeatQueryTemplate template) {
    BeatQueryTemplateResponse response = new BeatQueryTemplateResponse();
    response.setId(template.getId());
    response.setBeatId(template.getBeat().getId());
    response.setEndpointType(template.getEndpointType());
    response.setCategory(template.getCategory());
    response.setBeatTerms(template.getBeatTerms());
    response.setLangDefault(template.getLangDefault());
    response.setCountryDefault(template.getCountryDefault());
    response.setInDefault(template.getInDefault());
    response.setNullableFields(template.getNullableFields());
    response.setMaxDefault(template.getMaxDefault());
    response.setSortbyDefault(template.getSortbyDefault());
    response.setCreatedAt(template.getCreatedAt());
    response.setUpdatedAt(template.getUpdatedAt());
    return response;
  }
}
