package ai.journa.prcontrol.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final Map<String, Deque<Instant>> searchHistory = new ConcurrentHashMap<>();

    public void enforceSearchLimit(String actor, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return;
        }
        String key = actor != null ? actor : "anonymous";
        Deque<Instant> timestamps = searchHistory.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(1));
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Search rate limit exceeded");
            }
            timestamps.addLast(now);
        }
    }
}
