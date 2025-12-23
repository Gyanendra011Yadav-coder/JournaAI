package ai.journa.prcontrol.dto;

public class BeatResponse {
  private Long id;
  private String name;
  private String slug;
  private boolean active;

  public BeatResponse() {
  }

  public BeatResponse(Long id, String name, String slug, boolean active) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.active = active;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
