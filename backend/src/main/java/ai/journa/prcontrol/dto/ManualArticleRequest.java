package ai.journa.prcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class ManualArticleRequest {
  @NotNull
  private Long beatId;

  @NotBlank
  private String title;

  @NotBlank
  private String url;

  private String sourceName;

  private String author;

  private String summary;

  private Instant publishedAtUtc;

  public Long getBeatId() {
    return beatId;
  }

  public void setBeatId(Long beatId) {
    this.beatId = beatId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public Instant getPublishedAtUtc() {
    return publishedAtUtc;
  }

  public void setPublishedAtUtc(Instant publishedAtUtc) {
    this.publishedAtUtc = publishedAtUtc;
  }
}
