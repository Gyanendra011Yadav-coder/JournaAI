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
  private final PlaywrightHtmlService playwrightHtmlService;
  private final Map<String, CachedHtml> cache = new ConcurrentHashMap<>();
  private final Map<String, Instant> domainThrottle = new ConcurrentHashMap<>();

  public HtmlFetchService(@Qualifier("htmlRestTemplate") RestTemplate htmlRestTemplate,
                          EnrichmentProperties properties,
                          PlaywrightHtmlService playwrightHtmlService) {
    this.restTemplate = htmlRestTemplate;
    this.properties = properties;
    this.playwrightHtmlService = playwrightHtmlService;
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
    Optional<String> body = fetchStatic(url);
    if (body.isPresent()) {
      String html = body.get();
      if (shouldRender(html)) {
        Optional<String> rendered = playwrightHtmlService.renderHtml(url);
        if (rendered.isPresent()) {
          cacheHtml(url, rendered.get(), true);
          return rendered;
        }
      }
      cacheHtml(url, html, false);
      return Optional.of(html);
    }
    Optional<String> rendered = playwrightHtmlService.renderHtml(url);
    if (rendered.isPresent()) {
      cacheHtml(url, rendered.get(), true);
    }
    return rendered;
  }

  public Optional<String> fetchHtmlWithPlaywright(String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    if (!playwrightHtmlService.isAvailable()) {
      return Optional.empty();
    }
    throttle(url);
    Optional<String> rendered = playwrightHtmlService.renderHtml(url);
    rendered.ifPresent(html -> cacheHtml(url, html, true));
    return rendered;
  }

  public boolean wasRendered(String url) {
    CachedHtml cached = url != null ? cache.get(url) : null;
    return cached != null && cached.rendered;
  }

  private Optional<String> fetchStatic(String url) {
    try {
      String body = restTemplate.getForObject(URI.create(url), String.class);
      return Optional.ofNullable(body);
    } catch (RestClientException ex) {
      logger.debug("Html fetch failed url={} reason={}", url, ex.getMessage());
      return Optional.empty();
    }
  }

  private boolean shouldRender(String html) {
    if (!playwrightHtmlService.isAvailable()) {
      return false;
    }
    if (html == null || html.isBlank()) {
      return true;
    }
    int minChars = properties.getPlaywrightMinHtmlChars();
    return minChars > 0 && html.length() < minChars;
  }

  private Instant expiresAt() {
    return Instant.now().plus(Duration.ofMinutes(properties.getHtmlCacheMinutes()));
  }

  private void cacheHtml(String url, String html, boolean rendered) {
    cache.put(url, new CachedHtml(html, expiresAt(), rendered));
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
    private final boolean rendered;

    private CachedHtml(String html, Instant expiresAt, boolean rendered) {
      this.html = html;
      this.expiresAt = expiresAt;
      this.rendered = rendered;
    }

    private boolean isExpired() {
      return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
  }
}
