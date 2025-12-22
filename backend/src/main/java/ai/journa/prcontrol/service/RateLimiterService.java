package ai.journa.prcontrol.service;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
    private long nextAllowedTime = 0;

    public synchronized void throttle(long minIntervalMs) {
        long now = System.currentTimeMillis();
        if (now < nextAllowedTime) {
            try {
                Thread.sleep(nextAllowedTime - now);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        nextAllowedTime = System.currentTimeMillis() + minIntervalMs;
    }
}
