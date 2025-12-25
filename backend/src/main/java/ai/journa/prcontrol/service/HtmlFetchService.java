package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.EnrichmentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HtmlFetchService {
  private static final Logger logger = LoggerFactory.getLogger(HtmlFetchService.class);
  private final RestTemplate restTemplate;
  private final EnrichmentProperties properties;
  private final Map<String, CachedHtml> cache = new ConcurrentHashMap<>();
  private final Map<String, Instant> domainThrottle = new ConcurrentHashMap<>();

  public HtmlFetchService(@Qualifier("htmlRestTemplate") RestTemplate htmlRestTemplate,
                          EnrichmentProperties properties) {
    this.restTemplate = htmlRestTemplate;
    this.properties = properties;
  }

  public Optional<String> fetchHtml(String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    CachedHtml cached = cache.get(url);
    if (cached != null && !cached.isExpired()) {
      return Optional.ofNullable(cached.html);
    }
    throttle(url);
    try {
      String body = restTemplate.getForObject(URI.create(url), String.class);
      cache.put(url, new CachedHtml(body, Instant.now().plus(Duration.ofMinutes(properties.getHtmlCacheMinutes()))));
      return Optional.ofNullable(body);
    } catch (RestClientException ex) {
      logger.warn("Html fetch failed url={} reason={}", url, ex.getMessage());
      return Optional.empty();
    }
  }

  private void throttle(String url) {
    try {
      String host = URI.create(url).getHost();
      if (host == null) {
        return;
      }
      Instant last = domainThrottle.get(host);
      if (last != null) {
        long elapsed = Duration.between(last, Instant.now()).toMillis();
        long delay = properties.getPerDomainDelayMs() - elapsed;
        if (delay > 0) {
          try {
            Thread.sleep(delay);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        }
      }
      domainThrottle.put(host, Instant.now());
    } catch (Exception ex) {
      logger.debug("Unable to apply domain throttle for url={}", url);
    }
  }

  private static class CachedHtml {
    private final String html;
    private final Instant expiresAt;

    private CachedHtml(String html, Instant expiresAt) {
      this.html = html;
      this.expiresAt = expiresAt;
    }

    private boolean isExpired() {
      return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
  }
}
