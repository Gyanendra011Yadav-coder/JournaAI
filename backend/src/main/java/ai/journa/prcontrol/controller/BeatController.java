package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.BeatResponse;
import ai.journa.prcontrol.repository.BeatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/beats")
public class BeatController {
    private final BeatRepository beatRepository;

    public BeatController(BeatRepository beatRepository) {
        this.beatRepository = beatRepository;
    }

    @GetMapping
    public ResponseEntity<List<BeatResponse>> list() {
        List<BeatResponse> beats = beatRepository.findAll().stream().map(beat -> {
            BeatResponse response = new BeatResponse();
            response.setId(beat.getId());
            response.setName(beat.getName());
            response.setSlug(beat.getSlug());
            return response;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(beats);
    }
}
