package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "journalists")
public class Journalist {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "publication_name")
  private String publicationName;

  @Column(name = "publication_domain")
  private String publicationDomain;

  private String linkedin;

  private String designation;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "beats", columnDefinition = "text[]")
  private String[] beats;

  private String country;

  private String city;

  @Column(name = "journey_summary")
  private String journeySummary;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private JournalistStatus status = JournalistStatus.DRAFT;

  @Column(name = "author_page_url")
  private String authorPageUrl;

  private String twitter;

  @Column(name = "bio_summary")
  private String bioSummary;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private JournalistVerificationStatus verificationStatus = JournalistVerificationStatus.UNVERIFIED;

  @Column(name = "completeness_score", nullable = false)
  private int completenessScore = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "last_updated_at", nullable = false)
  private Instant lastUpdatedAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPublicationName() {
    return publicationName;
  }

  public void setPublicationName(String publicationName) {
    this.publicationName = publicationName;
  }

  public String getPublicationDomain() {
    return publicationDomain;
  }

  public void setPublicationDomain(String publicationDomain) {
    this.publicationDomain = publicationDomain;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public String getLinkedin() {
    return linkedin;
  }

  public void setLinkedin(String linkedin) {
    this.linkedin = linkedin;
  }

  public String[] getBeats() {
    return beats;
  }

  public void setBeats(String[] beats) {
    this.beats = beats;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getJourneySummary() {
    return journeySummary;
  }

  public void setJourneySummary(String journeySummary) {
    this.journeySummary = journeySummary;
  }

  public JournalistStatus getStatus() {
    return status;
  }

  public void setStatus(JournalistStatus status) {
    this.status = status;
  }

  public String getAuthorPageUrl() {
    return authorPageUrl;
  }

  public void setAuthorPageUrl(String authorPageUrl) {
    this.authorPageUrl = authorPageUrl;
  }

  public String getTwitter() {
    return twitter;
  }

  public void setTwitter(String twitter) {
    this.twitter = twitter;
  }

  public String getBioSummary() {
    return bioSummary;
  }

  public void setBioSummary(String bioSummary) {
    this.bioSummary = bioSummary;
  }

  public JournalistVerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(JournalistVerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public int getCompletenessScore() {
    return completenessScore;
  }

  public void setCompletenessScore(int completenessScore) {
    this.completenessScore = completenessScore;
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

  public Instant getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Instant lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
