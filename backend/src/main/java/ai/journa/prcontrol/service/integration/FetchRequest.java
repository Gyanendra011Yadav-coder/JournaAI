package ai.journa.prcontrol.service.integration;

import ai.journa.prcontrol.domain.EndpointType;

import java.time.Instant;

public class FetchRequest {
  private EndpointType endpointType;
  private String query;
  private String category;
  private String lang;
  private String country;
  private String inFields;
  private String nullableFields;
  private Integer max;
  private String sort;
  private Instant from;
  private Instant to;

  public EndpointType getEndpointType() {
    return endpointType;
  }

  public void setEndpointType(EndpointType endpointType) {
    this.endpointType = endpointType;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getInFields() {
    return inFields;
  }

  public void setInFields(String inFields) {
    this.inFields = inFields;
  }

  public String getNullableFields() {
    return nullableFields;
  }

  public void setNullableFields(String nullableFields) {
    this.nullableFields = nullableFields;
  }

  public Integer getMax() {
    return max;
  }

  public void setMax(Integer max) {
    this.max = max;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public Instant getFrom() {
    return from;
  }

  public void setFrom(Instant from) {
    this.from = from;
  }

  public Instant getTo() {
    return to;
  }

  public void setTo(Instant to) {
    this.to = to;
  }
}
