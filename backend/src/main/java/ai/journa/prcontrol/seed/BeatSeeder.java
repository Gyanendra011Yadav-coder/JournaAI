package ai.journa.prcontrol.seed;

import ai.journa.prcontrol.config.BeatProperties;
import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.BeatQueryRecipe;
import ai.journa.prcontrol.domain.EndpointType;
import ai.journa.prcontrol.domain.NewsFetchState;
import ai.journa.prcontrol.repository.BeatQueryRecipeRepository;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BeatSeeder implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(BeatSeeder.class);

  private final BeatRepository beatRepository;
  private final BeatQueryRecipeRepository beatQueryRecipeRepository;
  private final NewsFetchStateRepository newsFetchStateRepository;
  private final BeatProperties beatProperties;

  public BeatSeeder(BeatRepository beatRepository,
                    BeatQueryRecipeRepository beatQueryRecipeRepository,
                    NewsFetchStateRepository newsFetchStateRepository,
                    BeatProperties beatProperties) {
    this.beatRepository = beatRepository;
    this.beatQueryRecipeRepository = beatQueryRecipeRepository;
    this.newsFetchStateRepository = newsFetchStateRepository;
    this.beatProperties = beatProperties;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<BeatProperties.BeatDefinition> definitions = beatProperties.getBeats();
    if (definitions == null || definitions.isEmpty()) {
      logger.info("No beats configured; skipping beat seeding.");
      return;
    }

    List<Beat> existing = beatRepository.findAll();
    Map<String, Beat> bySlug = existing.stream()
        .filter(beat -> beat.getSlug() != null)
        .collect(Collectors.toMap(beat -> normalize(beat.getSlug()), Function.identity(), (left, right) -> left));
    Map<String, Beat> byName = existing.stream()
        .filter(beat -> beat.getName() != null)
        .collect(Collectors.toMap(beat -> normalize(beat.getName()), Function.identity(), (left, right) -> left));

    int created = 0;
    int updated = 0;
    int recipesCreated = 0;
    int statesCreated = 0;
    Set<String> seen = new HashSet<>();

    for (BeatProperties.BeatDefinition definition : definitions) {
      if (definition == null || definition.getName() == null || definition.getName().trim().isEmpty()) {
        continue;
      }
      String name = definition.getName().trim();
      String slug = definition.getSlug();
      if (slug == null || slug.trim().isEmpty()) {
        slug = slugify(name);
      } else {
        slug = slug.trim();
      }

      String slugKey = normalize(slug);
      if (!seen.add(slugKey)) {
        logger.warn("Duplicate beat slug '{}' in configuration; skipping.", slug);
        continue;
      }

      Beat beat = bySlug.getOrDefault(slugKey, byName.get(normalize(name)));
      boolean isNew = false;
      if (beat == null) {
        beat = new Beat();
        isNew = true;
      }

      beat.setName(name);
      beat.setSlug(slug);
      beat.setActive(definition.getActive() == null || definition.getActive());
      beatRepository.save(beat);

      if (isNew) {
        created++;
      } else {
        updated++;
      }

      if (beatQueryRecipeRepository.findByBeatId(beat.getId()).isEmpty()) {
        BeatQueryRecipe recipe = new BeatQueryRecipe();
        recipe.setBeat(beat);
        recipe.setEndpointType(EndpointType.SEARCH);
        recipe.setQuery(beat.getName());
        recipe.setSort("publishedAt");
        beatQueryRecipeRepository.save(recipe);
        recipesCreated++;
      }

      if (newsFetchStateRepository.findByBeatId(beat.getId()).isEmpty()) {
        NewsFetchState state = new NewsFetchState();
        state.setBeat(beat);
        state.setConsecutiveFailures(0);
        newsFetchStateRepository.save(state);
        statesCreated++;
      }
    }

    logger.info("Beat seeding complete. Created: {}, Updated: {}, Recipes: {}, States: {}.",
        created, updated, recipesCreated, statesCreated);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String slugify(String value) {
    String slug = normalize(value);
    slug = slug.replaceAll("[^a-z0-9]+", "-");
    slug = slug.replaceAll("(^-+|-+$)", "");
    return slug;
  }
}
