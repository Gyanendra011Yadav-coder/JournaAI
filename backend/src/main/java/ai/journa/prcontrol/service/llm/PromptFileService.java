package ai.journa.prcontrol.service.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PromptFileService {
  private static final Logger logger = LoggerFactory.getLogger(PromptFileService.class);
  private static final String PROMPT_PATH = "classpath:llm/prompts/promptFile.txt";
  private static final String AUTHOR_SYSTEM = "AUTHOR_EXTRACTION_SYSTEM";
  private static final String AUTHOR_USER = "AUTHOR_EXTRACTION_USER";
  private static final String JOURNALIST_SYSTEM = "JOURNALIST_ENRICHMENT_SYSTEM";
  private static final String JOURNALIST_USER = "JOURNALIST_ENRICHMENT_USER";

  private static final String DEFAULT_AUTHOR_SYSTEM = """
      You extract author attribution from news articles. Be conservative and precise.
      Return ONLY valid JSON matching the schema. No commentary.

      Rules:
      - If byline refers to organization/desk/agency/staff/bureau/team/company, classify as BUREAU and output no person authors.
      - If multiple person authors, output all.
      - If uncertain, classify UNKNOWN with low confidence.
      - Never invent names.

      Schema:
      {
        "classification": "PERSON|BUREAU|UNKNOWN",
        "authors": [{"name": "...", "confidence": 0-100, "evidence": "..."}],
        "bureau": {"name": "...", "confidence": 0-100, "evidence": "..."} | null,
        "needs_human_review": true|false
      }
      """;
  private static final String DEFAULT_AUTHOR_USER = """
      Article URL: {{ARTICLE_URL}}
      Publisher/Source: {{SOURCE_NAME}}

      Signals (may be empty):
      - JSON-LD snippets: {{JSONLD_SNIPPETS}}
      - Meta tags: {{META_TAGS}}
      - Visible byline snippet: {{BYLINE_SNIPPET}}
      - Other page text snippet: {{PAGE_TEXT_SNIPPET}}

      Extract the author attribution. Output JSON only.
      """;
  private static final String DEFAULT_JOURNALIST_SYSTEM = """
      You enrich journalist profiles using ONLY the provided verified evidence text.
      Do NOT guess email/phone. Do NOT fabricate details. Output JSON only.

      Schema:
      {
        "proposed_profile": {
          "full_name": "...",
          "publication_name": "...",
          "publication_domain": "...",
          "designation": "...|null",
          "beats": ["..."],
          "location": {"country":"...|null","city":"...|null"},
          "public_links": {"author_page":"...|null","twitter":"...|null","linkedin":"...|null"},
          "bio_summary": "2-4 lines|null"
        },
        "confidence": 0-100,
        "evidence": {"field": "supporting snippet or url"},
        "needs_admin_approval": true
      }
      """;
  private static final String DEFAULT_JOURNALIST_USER = """
      Current journalist record:
      {{CURRENT_JOURNALIST_JSON}}

      Verified evidence (may be empty):
      - Publisher author page URL: {{AUTHOR_PAGE_URL}}
      - Publisher author page text: {{AUTHOR_PAGE_TEXT}}
      - Recent article titles/urls: {{RECENT_ARTICLES}}

      Enrich cautiously. Output JSON only.
      """;

  private final ResourceLoader resourceLoader;
  private PromptSet promptSet = PromptSet.defaults();

  public PromptFileService(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @PostConstruct
  public void load() {
    Resource resource = resourceLoader.getResource(PROMPT_PATH);
    if (!resource.exists()) {
      logger.warn("Prompt file not found at {}, using defaults", PROMPT_PATH);
      return;
    }
    try {
      String content;
      try (var inputStream = resource.getInputStream()) {
        content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }
      PromptSet parsed = parsePromptFile(content);
      promptSet = parsed.withDefaults();
      logger.info("Prompt file loaded from {}", PROMPT_PATH);
    } catch (Exception ex) {
      logger.warn("Failed to load prompt file from {}, using defaults", PROMPT_PATH);
    }
  }

  public String getAuthorSystemPrompt() {
    return promptSet.authorSystem();
  }

  public String renderAuthorUserPrompt(Map<String, String> values) {
    return renderTemplate(promptSet.authorUser(), values);
  }

  public String getJournalistSystemPrompt() {
    return promptSet.journalistSystem();
  }

  public String renderJournalistUserPrompt(Map<String, String> values) {
    return renderTemplate(promptSet.journalistUser(), values);
  }

  private PromptSet parsePromptFile(String content) {
    Map<String, StringBuilder> sections = new LinkedHashMap<>();
    String currentKey = null;
    for (String rawLine : content.split("\\R", -1)) {
      String line = rawLine.stripTrailing();
      if (line.startsWith("## ")) {
        String key = line.substring(3).trim();
        currentKey = key;
        sections.putIfAbsent(key, new StringBuilder());
        continue;
      }
      if (currentKey != null) {
        sections.get(currentKey).append(line).append("\n");
      }
    }
    return new PromptSet(
        section(sections, AUTHOR_SYSTEM),
        section(sections, AUTHOR_USER),
        section(sections, JOURNALIST_SYSTEM),
        section(sections, JOURNALIST_USER)
    );
  }

  private String section(Map<String, StringBuilder> sections, String key) {
    StringBuilder builder = sections.get(key);
    return builder == null ? null : builder.toString().trim();
  }

  private String renderTemplate(String template, Map<String, String> values) {
    if (template == null || template.isBlank()) {
      return "";
    }
    String rendered = template;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", safe(entry.getValue()));
    }
    return rendered;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private record PromptSet(String authorSystem,
                           String authorUser,
                           String journalistSystem,
                           String journalistUser) {
    static PromptSet defaults() {
      return new PromptSet(
          DEFAULT_AUTHOR_SYSTEM.trim(),
          DEFAULT_AUTHOR_USER.trim(),
          DEFAULT_JOURNALIST_SYSTEM.trim(),
          DEFAULT_JOURNALIST_USER.trim()
      );
    }

    PromptSet withDefaults() {
      return new PromptSet(
          authorSystem == null || authorSystem.isBlank() ? DEFAULT_AUTHOR_SYSTEM.trim() : authorSystem,
          authorUser == null || authorUser.isBlank() ? DEFAULT_AUTHOR_USER.trim() : authorUser,
          journalistSystem == null || journalistSystem.isBlank() ? DEFAULT_JOURNALIST_SYSTEM.trim() : journalistSystem,
          journalistUser == null || journalistUser.isBlank() ? DEFAULT_JOURNALIST_USER.trim() : journalistUser
      );
    }
  }
}
