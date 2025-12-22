package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.OutreachTemplate;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.OutreachTemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final BeatRepository beatRepository;
    private final OutreachTemplateRepository outreachTemplateRepository;

    public AdminController(BeatRepository beatRepository,
                           OutreachTemplateRepository outreachTemplateRepository) {
        this.beatRepository = beatRepository;
        this.outreachTemplateRepository = outreachTemplateRepository;
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        if (beatRepository.count() == 0) {
            List.of("Taxation", "Markets", "Legal", "Technology").forEach(name -> {
                Beat beat = new Beat();
                beat.setName(name);
                beatRepository.save(beat);
            });
        }
        if (outreachTemplateRepository.count() == 0) {
            OutreachTemplate template = new OutreachTemplate();
            template.setName("Default Outreach");
            template.setSubject("Story idea following {{headline}}");
            template.setBody("Hi {{journalistName}},\n\nI saw your piece on {{headline}}. Our client shared this quote: '{{clientQuote}}'. Would you like to chat?\n\nBest,\n{{spokespersonName}}");
            outreachTemplateRepository.save(template);
        }
        return ResponseEntity.ok("Seeded");
    }
}
