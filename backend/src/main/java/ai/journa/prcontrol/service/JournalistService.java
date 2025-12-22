package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.repository.JournalistRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JournalistService {
    private final JournalistRepository journalistRepository;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;
    private final NewsProviderProperties newsProviderProperties;

    public JournalistService(JournalistRepository journalistRepository,
                             AuditService auditService,
                             RateLimiterService rateLimiterService,
                             NewsProviderProperties newsProviderProperties) {
        this.journalistRepository = journalistRepository;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
        this.newsProviderProperties = newsProviderProperties;
    }

    public List<Journalist> search(String actor, String beat, String outlet, String location, String keywords) {
        rateLimiterService.enforceSearchLimit(actor, newsProviderProperties.getSearchesPerMinute());
        auditService.record(actor, "SEARCH", "journalists", "{\"beat\":\"" + beat + "\"}");
        List<Journalist> results = journalistRepository.search(emptyToNull(beat), emptyToNull(outlet), emptyToNull(location));
        if (keywords == null || keywords.isBlank()) {
            return results;
        }
        String keywordLower = keywords.toLowerCase();
        return results.stream()
                .filter(journalist -> journalist.getName().toLowerCase().contains(keywordLower)
                        || (journalist.getOutlet() != null && journalist.getOutlet().toLowerCase().contains(keywordLower)))
                .collect(Collectors.toList());
    }

    public Journalist get(Long id) {
        return journalistRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Journalist not found"));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
