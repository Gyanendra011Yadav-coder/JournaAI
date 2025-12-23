package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.BeatQueryRecipe;
import ai.journa.prcontrol.dto.BeatQueryRecipeRequest;
import ai.journa.prcontrol.dto.BeatQueryRecipeResponse;
import ai.journa.prcontrol.service.BeatQueryRecipeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/beat-query-recipes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBeatQueryRecipeController {
  private final BeatQueryRecipeService beatQueryRecipeService;

  public AdminBeatQueryRecipeController(BeatQueryRecipeService beatQueryRecipeService) {
    this.beatQueryRecipeService = beatQueryRecipeService;
  }

  @GetMapping
  public List<BeatQueryRecipeResponse> list(@RequestParam(required = false) Long beatId) {
    List<BeatQueryRecipe> recipes = beatId != null
        ? beatQueryRecipeService.findByBeat(beatId)
        : beatQueryRecipeService.findAll();
    return recipes.stream().map(this::toResponse).toList();
  }

  @PostMapping
  public BeatQueryRecipeResponse create(@Valid @RequestBody BeatQueryRecipeRequest request) {
    return toResponse(beatQueryRecipeService.create(request));
  }

  @PutMapping("/{id}")
  public BeatQueryRecipeResponse update(@PathVariable Long id, @Valid @RequestBody BeatQueryRecipeRequest request) {
    return toResponse(beatQueryRecipeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    beatQueryRecipeService.delete(id);
  }

  private BeatQueryRecipeResponse toResponse(BeatQueryRecipe recipe) {
    BeatQueryRecipeResponse response = new BeatQueryRecipeResponse();
    response.setId(recipe.getId());
    response.setBeatId(recipe.getBeat().getId());
    response.setEndpointType(recipe.getEndpointType());
    response.setQuery(recipe.getQuery());
    response.setCategory(recipe.getCategory());
    response.setLang(recipe.getLang());
    response.setCountry(recipe.getCountry());
    response.setInFields(recipe.getInFields());
    response.setNullableFields(recipe.getNullableFields());
    response.setMax(recipe.getMax());
    response.setSort(recipe.getSort());
    response.setCreatedAt(recipe.getCreatedAt());
    response.setUpdatedAt(recipe.getUpdatedAt());
    return response;
  }
}
