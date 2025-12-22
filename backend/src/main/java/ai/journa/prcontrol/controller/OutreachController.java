package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.OutreachEmail;
import ai.journa.prcontrol.dto.OutreachComposeRequest;
import ai.journa.prcontrol.dto.OutreachEmailResponse;
import ai.journa.prcontrol.service.OutreachService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outreach")
public class OutreachController {
    private final OutreachService outreachService;

    public OutreachController(OutreachService outreachService) {
        this.outreachService = outreachService;
    }

    @PostMapping("/send")
    public ResponseEntity<OutreachEmailResponse> send(@Valid @RequestBody OutreachComposeRequest request,
                                                      Authentication authentication) {
        OutreachEmail email = outreachService.send(authentication.getName(), request);
        OutreachEmailResponse response = new OutreachEmailResponse();
        response.setId(email.getId());
        response.setStatus(email.getStatus());
        response.setSentAt(email.getSentAt());
        response.setProviderMessageId(email.getProviderMessageId());
        return ResponseEntity.ok(response);
    }
}
