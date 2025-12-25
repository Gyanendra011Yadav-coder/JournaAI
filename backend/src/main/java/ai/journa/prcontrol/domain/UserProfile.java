package ai.journa.prcontrol.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_profile")
public class UserProfile {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "preferred_countries", columnDefinition = "text[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] preferredCountries;

  @Column(name = "preferred_langs", columnDefinition = "text[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] preferredLangs;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_sidebar_mode")
  private SidebarMode defaultSidebarMode = SidebarMode.TRENDING;

  @Column(name = "client_lens_ratio", nullable = false)
  private int clientLensRatio = 40;

  @Column(name = "trending_local_ratio", nullable = false)
  private int trendingLocalRatio = 40;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @ManyToMany
  @JoinTable(
      name = "user_profile_beats",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "beat_id")
  )
  private Set<Beat> beats = new HashSet<>();

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String[] getPreferredCountries() {
    return preferredCountries;
  }

  public void setPreferredCountries(String[] preferredCountries) {
    this.preferredCountries = preferredCountries;
  }

  public String[] getPreferredLangs() {
    return preferredLangs;
  }

  public void setPreferredLangs(String[] preferredLangs) {
    this.preferredLangs = preferredLangs;
  }

  public SidebarMode getDefaultSidebarMode() {
    return defaultSidebarMode;
  }

  public void setDefaultSidebarMode(SidebarMode defaultSidebarMode) {
    this.defaultSidebarMode = defaultSidebarMode;
  }

  public int getClientLensRatio() {
    return clientLensRatio;
  }

  public void setClientLensRatio(int clientLensRatio) {
    this.clientLensRatio = clientLensRatio;
  }

  public int getTrendingLocalRatio() {
    return trendingLocalRatio;
  }

  public void setTrendingLocalRatio(int trendingLocalRatio) {
    this.trendingLocalRatio = trendingLocalRatio;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Set<Beat> getBeats() {
    return beats;
  }

  public void setBeats(Set<Beat> beats) {
    this.beats = beats;
  }
}
