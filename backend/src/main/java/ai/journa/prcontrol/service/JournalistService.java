package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.repository.JournalistRepository;
import ai.journa.prcontrol.service.integration.MediaDatabaseProvider;
import ai.journa.prcontrol.service.integration.model.MediaJournalist;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JournalistService {
    private final MediaDatabaseProvider mediaDatabaseProvider;
    private final JournalistRepository journalistRepository;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public JournalistService(MediaDatabaseProvider mediaDatabaseProvider,
                             JournalistRepository journalistRepository,
                             AuditService auditService,
                             RateLimiterService rateLimiterService) {
        this.mediaDatabaseProvider = mediaDatabaseProvider;
        this.journalistRepository = journalistRepository;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    public List<Journalist> search(String actor, String beat, String outlet, String location, String keywords) {
        auditService.record(actor, "SEARCH", "journalists", "{\"beat\":\"" + beat + "\"}");
        List<MediaJournalist> results = withRetry(() -> {
            rateLimiterService.throttle(300);
            return mediaDatabaseProvider.searchJournalists(beat, outlet, location, keywords);
        });
        return results.stream().map(result -> {
            Journalist journalist = new Journalist();
            journalist.setName(result.getName());
            journalist.setOutlet(result.getOutlet());
            journalist.setBeatTags(result.getBeatTags());
            journalist.setLocation(result.getLocation());
            journalist.setEmail(result.getEmail());
            journalist.setSourceProvider("mock");
            journalist.setProviderReferenceId(result.getId());
            return journalistRepository.save(journalist);
        }).collect(Collectors.toList());
    }

    public Journalist get(Long id) {
        return journalistRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Journalist not found"));
    }

    private List<MediaJournalist> withRetry(ProviderCall call) {
        int attempts = 0;
        RuntimeException last = null;
        while (attempts < 3) {
            try {
                return call.get();
            } catch (RuntimeException ex) {
                last = ex;
                attempts++;
            }
        }
        throw last != null ? last : new IllegalStateException("Unable to fetch journalists");
    }

    @FunctionalInterface
    private interface ProviderCall {
        List<MediaJournalist> get();
    }
}
