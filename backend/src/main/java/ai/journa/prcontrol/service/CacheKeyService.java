package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.IngestMode;
import org.springframework.stereotype.Service;

@Service
public class CacheKeyService {
  public String build(IngestMode mode,
                      String lensOrTrack,
                      Long beatId,
                      String category,
                      String query,
                      AppLocaleResolver.Resolution locale) {
    return String.join(":",
        mode.name(),
        lensOrTrack != null ? lensOrTrack : "ALL",
        beatId != null ? "beat=" + beatId : "beat=none",
        category != null ? "category=" + category : "category=none",
        query != null ? "q=" + query : "q=none",
        locale.country() != null ? "country=" + locale.country() : "country=none",
        locale.lang() != null ? "lang=" + locale.lang() : "lang=none");
  }
}
