package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "journalist_contacts")
public class JournalistContact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalist_id", nullable = false)
  private Journalist journalist;

  private String email;

  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContactVisibility visibility = ContactVisibility.PUBLIC;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private ContactSourceType sourceType = ContactSourceType.MANUAL;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by")
  private User verifiedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Journalist getJournalist() {
    return journalist;
  }

  public void setJournalist(Journalist journalist) {
    this.journalist = journalist;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public ContactVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(ContactVisibility visibility) {
    this.visibility = visibility;
  }

  public ContactSourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(ContactSourceType sourceType) {
    this.sourceType = sourceType;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(Instant verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public User getVerifiedBy() {
    return verifiedBy;
  }

  public void setVerifiedBy(User verifiedBy) {
    this.verifiedBy = verifiedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
