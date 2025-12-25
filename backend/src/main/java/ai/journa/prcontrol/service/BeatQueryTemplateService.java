package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryTemplate;
import ai.journa.prcontrol.dto.BeatQueryTemplateRequest;
import ai.journa.prcontrol.repository.BeatQueryTemplateRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BeatQueryTemplateService {
  private final BeatQueryTemplateRepository templateRepository;
  private final BeatRepository beatRepository;

  public BeatQueryTemplateService(BeatQueryTemplateRepository templateRepository, BeatRepository beatRepository) {
    this.templateRepository = templateRepository;
    this.beatRepository = beatRepository;
  }

  public List<BeatQueryTemplate> findAll() {
    return templateRepository.findAll();
  }

  public List<BeatQueryTemplate> findByBeat(Long beatId) {
    return templateRepository.findByBeatId(beatId);
  }

  public BeatQueryTemplate create(BeatQueryTemplateRequest request) {
    Beat beat = beatRepository.findById(request.getBeatId())
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    BeatQueryTemplate template = new BeatQueryTemplate();
    apply(template, beat, request);
    return templateRepository.save(template);
  }

  public BeatQueryTemplate update(Long id, BeatQueryTemplateRequest request) {
    BeatQueryTemplate template = templateRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Template not found"));
    Beat beat = beatRepository.findById(request.getBeatId())
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    apply(template, beat, request);
    template.setUpdatedAt(Instant.now());
    return templateRepository.save(template);
  }

  public void delete(Long id) {
    templateRepository.deleteById(id);
  }

  private void apply(BeatQueryTemplate template, Beat beat, BeatQueryTemplateRequest request) {
    template.setBeat(beat);
    template.setEndpointType(request.getEndpointType());
    template.setCategory(request.getCategory());
    template.setBeatTerms(request.getBeatTerms());
    template.setLangDefault(request.getLangDefault());
    template.setCountryDefault(request.getCountryDefault());
    template.setInDefault(request.getInDefault());
    template.setNullableFields(request.getNullableFields());
    template.setMaxDefault(request.getMaxDefault());
    template.setSortbyDefault(request.getSortbyDefault());
  }
}
