package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "beat_query_recipes")
public class BeatQueryRecipe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id", nullable = false)
  private Beat beat;

  @Enumerated(EnumType.STRING)
  @Column(name = "endpoint_type", nullable = false)
  private EndpointType endpointType;

  @Column(name = "q")
  private String query;

  @Column(name = "category")
  private String category;

  @Column(name = "lang")
  private String lang;

  @Column(name = "country")
  private String country;

  @Column(name = "in_fields")
  private String inFields;

  @Column(name = "nullable_fields")
  private String nullableFields;

  @Column(name = "max")
  private Integer max;

  @Column(name = "sort")
  private String sort;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Beat getBeat() {
    return beat;
  }

  public void setBeat(Beat beat) {
    this.beat = beat;
  }

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
