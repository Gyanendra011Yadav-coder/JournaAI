package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.dto.BeatRequest;
import ai.journa.prcontrol.dto.BeatResponse;
import ai.journa.prcontrol.service.BeatService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/beats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBeatController {
  private final BeatService beatService;

  public AdminBeatController(BeatService beatService) {
    this.beatService = beatService;
  }

  @GetMapping
  public List<BeatResponse> listBeats() {
    return beatService.findAll().stream().map(this::toResponse).toList();
  }

  @PostMapping
  public BeatResponse create(@Valid @RequestBody BeatRequest request) {
    return toResponse(beatService.create(request));
  }

  @PutMapping("/{id}")
  public BeatResponse update(@PathVariable Long id, @Valid @RequestBody BeatRequest request) {
    return toResponse(beatService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    beatService.delete(id);
  }

  private BeatResponse toResponse(Beat beat) {
    return new BeatResponse(beat.getId(), beat.getName(), beat.getSlug(), beat.isActive());
  }
}
