package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.OutreachTemplate;
import ai.journa.prcontrol.service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/api/templates")
    public ResponseEntity<List<OutreachTemplate>> list() {
        return ResponseEntity.ok(templateService.list());
    }
}
