package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.OutreachTemplate;
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
}
