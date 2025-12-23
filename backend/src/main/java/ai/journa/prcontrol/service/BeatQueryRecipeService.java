package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryRecipe;
import ai.journa.prcontrol.dto.BeatQueryRecipeRequest;
import ai.journa.prcontrol.repository.BeatQueryRecipeRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BeatQueryRecipeService {
  private final BeatQueryRecipeRepository recipeRepository;
  private final BeatRepository beatRepository;

  public BeatQueryRecipeService(BeatQueryRecipeRepository recipeRepository, BeatRepository beatRepository) {
    this.recipeRepository = recipeRepository;
    this.beatRepository = beatRepository;
  }

  public List<BeatQueryRecipe> findAll() {
    return recipeRepository.findAll();
  }

  public List<BeatQueryRecipe> findByBeat(Long beatId) {
    return recipeRepository.findByBeatId(beatId);
  }

  public BeatQueryRecipe create(BeatQueryRecipeRequest request) {
    Beat beat = beatRepository.findById(request.getBeatId())
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    BeatQueryRecipe recipe = new BeatQueryRecipe();
    apply(recipe, beat, request);
    return recipeRepository.save(recipe);
  }

  public BeatQueryRecipe update(Long id, BeatQueryRecipeRequest request) {
    BeatQueryRecipe recipe = recipeRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Recipe not found"));
    Beat beat = beatRepository.findById(request.getBeatId())
        .orElseThrow(() -> new IllegalStateException("Beat not found"));
    apply(recipe, beat, request);
    recipe.setUpdatedAt(Instant.now());
    return recipeRepository.save(recipe);
  }

  public void delete(Long id) {
    recipeRepository.deleteById(id);
  }

  private void apply(BeatQueryRecipe recipe, Beat beat, BeatQueryRecipeRequest request) {
    recipe.setBeat(beat);
    recipe.setEndpointType(request.getEndpointType());
    recipe.setQuery(request.getQuery());
    recipe.setCategory(request.getCategory());
    recipe.setLang(request.getLang());
    recipe.setCountry(request.getCountry());
    recipe.setInFields(request.getInFields());
    recipe.setNullableFields(request.getNullableFields());
    recipe.setMax(request.getMax());
    recipe.setSort(request.getSort());
  }
}
