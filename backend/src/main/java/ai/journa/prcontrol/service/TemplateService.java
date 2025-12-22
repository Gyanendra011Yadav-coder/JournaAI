package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.OutreachTemplate;
import ai.journa.prcontrol.dto.TemplateRequest;
import ai.journa.prcontrol.repository.OutreachTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {
    private final OutreachTemplateRepository outreachTemplateRepository;

    public TemplateService(OutreachTemplateRepository outreachTemplateRepository) {
        this.outreachTemplateRepository = outreachTemplateRepository;
    }

    public List<OutreachTemplate> list() {
        return outreachTemplateRepository.findAll();
    }

    public OutreachTemplate create(TemplateRequest request) {
        OutreachTemplate template = new OutreachTemplate();
        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        return outreachTemplateRepository.save(template);
    }
}
