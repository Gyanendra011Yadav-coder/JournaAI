package ai.journa.prcontrol.dto;

public class BeatStatusResponse {
  private Long id;
  private String name;
  private String lastRefreshedAt;

  public BeatStatusResponse(Long id, String name, String lastRefreshedAt) {
    this.id = id;
    this.name = name;
    this.lastRefreshedAt = lastRefreshedAt;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLastRefreshedAt() {
    return lastRefreshedAt;
  }
}
