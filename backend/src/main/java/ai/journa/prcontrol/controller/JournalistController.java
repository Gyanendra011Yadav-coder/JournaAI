package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.dto.JournalistResponse;
import ai.journa.prcontrol.service.JournalistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journalists")
public class JournalistController {
    private final JournalistService journalistService;

    public JournalistController(JournalistService journalistService) {
        this.journalistService = journalistService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<JournalistResponse>> search(@RequestParam String beat,
                                                           @RequestParam(required = false) String outlet,
                                                           @RequestParam(required = false) String location,
                                                           @RequestParam(required = false) String keywords,
                                                           Authentication authentication) {
        List<Journalist> journalists = journalistService.search(authentication.getName(), beat, outlet, location, keywords);
        return ResponseEntity.ok(journalists.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalistResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(journalistService.get(id)));
    }

    private JournalistResponse toResponse(Journalist journalist) {
        JournalistResponse response = new JournalistResponse();
        response.setId(journalist.getId());
        response.setName(journalist.getName());
        response.setOutlet(journalist.getOutlet());
        response.setBeatTags(journalist.getBeatTags());
        response.setLocation(journalist.getLocation());
        response.setEmail(journalist.getEmail());
        return response;
    }
}
