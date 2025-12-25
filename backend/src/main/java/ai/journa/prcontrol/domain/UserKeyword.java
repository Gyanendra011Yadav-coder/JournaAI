package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_keywords")
public class UserKeyword {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  private UserKeywordKind kind;

  @Column(name = "keyword", nullable = false)
  private String keyword;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public UserKeywordKind getKind() {
    return kind;
  }

  public void setKind(UserKeywordKind kind) {
    this.kind = kind;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
}
