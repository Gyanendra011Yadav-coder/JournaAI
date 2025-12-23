package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.dto.BeatResponse;
import ai.journa.prcontrol.service.BeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beats")
public class BeatController {
  private final BeatService beatService;

  public BeatController(BeatService beatService) {
    this.beatService = beatService;
  }

  @GetMapping
  public List<BeatResponse> listBeats() {
    return beatService.findAllActive().stream()
        .map(this::toResponse)
        .toList();
  }

  private BeatResponse toResponse(Beat beat) {
    return new BeatResponse(beat.getId(), beat.getName(), beat.getSlug(), beat.isActive());
  }
}
