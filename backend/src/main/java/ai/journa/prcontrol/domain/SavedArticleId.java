package ai.journa.prcontrol.domain;

import java.io.Serializable;
import java.util.Objects;

public class SavedArticleId implements Serializable {
  private Long user;
  private Long article;

  public SavedArticleId() {}

  public SavedArticleId(Long user, Long article) {
    this.user = user;
    this.article = article;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SavedArticleId that = (SavedArticleId) o;
    return Objects.equals(user, that.user) && Objects.equals(article, that.article);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, article);
  }
}
