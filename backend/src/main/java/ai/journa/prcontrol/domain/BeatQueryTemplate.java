package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "beat_query_templates")
public class BeatQueryTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id", nullable = false)
  private Beat beat;

  @Enumerated(EnumType.STRING)
  @Column(name = "endpoint_type", nullable = false)
  private EndpointType endpointType;

  @Column(name = "category")
  private String category;

  @Column(name = "beat_terms_jsonb", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<String> beatTerms;

  @Column(name = "lang_default")
  private String langDefault;

  @Column(name = "country_default")
  private String countryDefault;

  @Column(name = "in_default")
  private String inDefault;

  @Column(name = "nullable_fields")
  private String nullableFields;

  @Column(name = "max_default")
  private Integer maxDefault;

  @Column(name = "sortby_default")
  private String sortbyDefault;

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
