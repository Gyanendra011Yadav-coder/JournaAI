package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;

public class BeatRequest {
  @NotBlank
  private String name;

  @NotBlank
  private String slug;

  private boolean active = true;

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

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
