package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.config.NewsProviderProperties;
import ai.journa.prcontrol.dto.IntegrationStatusResponse;
import ai.journa.prcontrol.repository.NewsFetchStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final NewsProviderProperties properties;
    private final NewsFetchStateRepository newsFetchStateRepository;

    public SettingsController(NewsProviderProperties properties,
                              NewsFetchStateRepository newsFetchStateRepository) {
        this.properties = properties;
        this.newsFetchStateRepository = newsFetchStateRepository;
    }

    @GetMapping("/integrations")
    public ResponseEntity<IntegrationStatusResponse> integrations() {
        IntegrationStatusResponse response = new IntegrationStatusResponse();
        response.setGnewsEnabled(properties.getGnews().getApiKey() != null && !properties.getGnews().getApiKey().isBlank());
        response.setRssFeedCount(properties.getRss().getFeeds() != null
                ? properties.getRss().getFeeds().values().stream().mapToInt(list -> list.size()).sum()
                : 0);
        Map<String, String> circuits = new HashMap<>();
        newsFetchStateRepository.findAll().forEach(state -> {
            Instant openUntil = state.getCircuitOpenUntil();
            if (openUntil != null && openUntil.isAfter(Instant.now())) {
                circuits.put(state.getBeat().getName() + ":" + state.getTimeframe(), "OPEN until " + openUntil);
            }
        });
        response.setCircuitStatus(circuits);
        return ResponseEntity.ok(response);
    }
}
