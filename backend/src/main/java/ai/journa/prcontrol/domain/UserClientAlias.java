package ai.journa.prcontrol.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_client_aliases")
public class UserClientAlias {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private UserClient client;

  @Column(name = "alias", nullable = false)
  private String alias;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UserClient getClient() {
    return client;
  }

  public void setClient(UserClient client) {
    this.client = client;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }
}
