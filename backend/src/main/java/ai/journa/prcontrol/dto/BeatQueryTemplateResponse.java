package ai.journa.prcontrol.dto;

import ai.journa.prcontrol.domain.EndpointType;

import java.time.Instant;
import java.util.List;

public class BeatQueryTemplateResponse {
  private Long id;
  private Long beatId;
  private EndpointType endpointType;
  private String category;
  private List<String> beatTerms;
  private String langDefault;
  private String countryDefault;
  private String inDefault;
  private String nullableFields;
  private Integer maxDefault;
  private String sortbyDefault;
  private Instant createdAt;
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBeatId() {
    return beatId;
  }

  public void setBeatId(Long beatId) {
    this.beatId = beatId;
  }

  public EndpointType getEndpointType() {
    return endpointType;
  }

  public void setEndpointType(EndpointType endpointType) {
    this.endpointType = endpointType;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public List<String> getBeatTerms() {
    return beatTerms;
  }

  public void setBeatTerms(List<String> beatTerms) {
    this.beatTerms = beatTerms;
  }

  public String getLangDefault() {
    return langDefault;
  }

  public void setLangDefault(String langDefault) {
    this.langDefault = langDefault;
  }

  public String getCountryDefault() {
    return countryDefault;
  }

  public void setCountryDefault(String countryDefault) {
    this.countryDefault = countryDefault;
  }

  public String getInDefault() {
    return inDefault;
  }

  public void setInDefault(String inDefault) {
    this.inDefault = inDefault;
  }

  public String getNullableFields() {
    return nullableFields;
  }

  public void setNullableFields(String nullableFields) {
    this.nullableFields = nullableFields;
  }

  public Integer getMaxDefault() {
    return maxDefault;
  }

  public void setMaxDefault(Integer maxDefault) {
    this.maxDefault = maxDefault;
  }

  public String getSortbyDefault() {
    return sortbyDefault;
  }

  public void setSortbyDefault(String sortbyDefault) {
    this.sortbyDefault = sortbyDefault;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
