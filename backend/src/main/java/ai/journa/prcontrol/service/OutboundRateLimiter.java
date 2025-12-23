package ai.journa.prcontrol.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class OutboundRateLimiter {
  private Instant nextAvailable = Instant.EPOCH;

  public synchronized void acquire() {
    Instant now = Instant.now();
    if (now.isBefore(nextAvailable)) {
      Duration wait = Duration.between(now, nextAvailable);
      try {
        Thread.sleep(wait.toMillis());
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    nextAvailable = Instant.now().plusSeconds(1);
  }
}
