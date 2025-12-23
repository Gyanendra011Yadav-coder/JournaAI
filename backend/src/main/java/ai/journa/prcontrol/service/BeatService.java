package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.dto.BeatRequest;
import ai.journa.prcontrol.repository.BeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeatService {
  private final BeatRepository beatRepository;

  public BeatService(BeatRepository beatRepository) {
    this.beatRepository = beatRepository;
  }

  public List<Beat> findAllActive() {
    return beatRepository.findAll()
        .stream()
        .filter(Beat::isActive)
        .toList();
  }

  public List<Beat> findAll() {
    return beatRepository.findAll();
  }

  public Beat create(BeatRequest request) {
    Beat beat = new Beat();
    beat.setName(request.getName());
    beat.setSlug(request.getSlug());
    beat.setActive(request.isActive());
    return beatRepository.save(beat);
  }

  public Beat update(Long id, BeatRequest request) {
    Beat beat = beatRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    beat.setName(request.getName());
    beat.setSlug(request.getSlug());
    beat.setActive(request.isActive());
    return beatRepository.save(beat);
  }

  public void delete(Long id) {
    beatRepository.deleteById(id);
  }
}
