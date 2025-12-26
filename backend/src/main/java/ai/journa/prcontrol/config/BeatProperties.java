package ai.journa.prcontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class BeatProperties {
  private List<BeatDefinition> beats = new ArrayList<>();

  public List<BeatDefinition> getBeats() {
    return beats;
  }

  public void setBeats(List<BeatDefinition> beats) {
    this.beats = beats;
  }

  public static class BeatDefinition {
    private String name;
    private String slug;
    private Boolean active;
    private List<String> terms;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getSlug() {
      return slug;
    }

    public void setSlug(String slug) {
      this.slug = slug;
    }

    public Boolean getActive() {
      return active;
    }

    public void setActive(Boolean active) {
      this.active = active;
    }

    public List<String> getTerms() {
      return terms;
    }

    public void setTerms(List<String> terms) {
      this.terms = terms;
    }
  }
}
