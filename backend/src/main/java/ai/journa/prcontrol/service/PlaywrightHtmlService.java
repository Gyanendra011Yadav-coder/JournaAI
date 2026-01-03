package ai.journa.prcontrol.service;

import ai.journa.prcontrol.config.EnrichmentProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;

@Service
public class PlaywrightHtmlService {
  private static final Logger logger = LoggerFactory.getLogger(PlaywrightHtmlService.class);
  private final EnrichmentProperties properties;
  private Playwright playwright;
  private Browser browser;
  private boolean available;
  private boolean initialized;

  public PlaywrightHtmlService(EnrichmentProperties properties) {
    this.properties = properties;
  }

  public Optional<String> renderHtml(String url) {
    if (url == null || url.isBlank() || !ensureInitialized()) {
      return Optional.empty();
    }
    BrowserContext context = null;
    Page page = null;
    Instant start = Instant.now();
    try {
      context = browser.newContext(new Browser.NewContextOptions()
          .setBypassCSP(true)
          .setViewportSize(1280, 720));
      page = context.newPage();
      page.navigate(url, new Page.NavigateOptions()
          .setWaitUntil(WaitUntilState.NETWORKIDLE)
          .setTimeout(properties.getPlaywrightTimeoutSeconds() * 1000L));
      int delay = properties.getPlaywrightPostLoadDelayMs();
      if (delay > 0) {
        page.waitForTimeout(delay);
      }
      String content = page.content();
      long durationMs = Duration.between(start, Instant.now()).toMillis();
      if (content == null || content.isBlank()) {
        logger.info("Playwright render empty url={} durationMs={}", url, durationMs);
        return Optional.empty();
      }
      logger.info("Playwright render ok url={} chars={} durationMs={}", url, content.length(), durationMs);
      return Optional.of(content);
    } catch (Exception ex) {
      logger.warn("Playwright render failed url={} reason={}", url, ex.getMessage());
      return Optional.empty();
    } finally {
      if (page != null) {
        page.close();
      }
      if (context != null) {
        context.close();
      }
    }
  }

  public boolean isAvailable() {
    return available;
  }

  private synchronized boolean ensureInitialized() {
    if (initialized) {
      return available;
    }
    initialized = true;
    initialize();
    return available;
  }

  private void initialize() {
    if (!properties.isPlaywrightEnabled()) {
      available = false;
      return;
    }
    Path cacheDir = resolveBrowserCacheDir();
    if (!isBrowserInstalled(cacheDir)) {
      available = false;
      logger.warn("Playwright browsers not installed. Run ./gradlew installPlaywright to preinstall.");
      return;
    }
    try {
      Map<String, String> env = Map.of(
          "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1",
          "PLAYWRIGHT_BROWSERS_PATH", cacheDir.toString()
      );
      playwright = Playwright.create(new Playwright.CreateOptions().setEnv(env));
      browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      available = true;
      logger.info("Playwright initialized");
    } catch (Exception ex) {
      available = false;
      logger.warn("Playwright disabled reason={}", ex.getMessage());
    }
  }

  @PreDestroy
  public void shutdown() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  private Path resolveBrowserCacheDir() {
    String override = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
    if (override != null && !override.isBlank() && !"0".equals(override)) {
      return Paths.get(override);
    }
    String home = System.getProperty("user.home");
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("mac")) {
      return Paths.get(home, "Library", "Caches", "ms-playwright");
    }
    if (os.contains("win")) {
      String localAppData = System.getenv("LOCALAPPDATA");
      if (localAppData != null && !localAppData.isBlank()) {
        return Paths.get(localAppData, "ms-playwright");
      }
      String appData = System.getenv("APPDATA");
      if (appData != null && !appData.isBlank()) {
        return Paths.get(appData, "ms-playwright");
      }
      return Paths.get(home, "AppData", "Local", "ms-playwright");
    }
    String xdg = System.getenv("XDG_CACHE_HOME");
    if (xdg != null && !xdg.isBlank()) {
      return Paths.get(xdg, "ms-playwright");
    }
    return Paths.get(home, ".cache", "ms-playwright");
  }

  private boolean isBrowserInstalled(Path cacheDir) {
    if (cacheDir == null || !Files.isDirectory(cacheDir)) {
      return false;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, "chromium-*")) {
      return stream.iterator().hasNext();
    } catch (Exception ex) {
      return false;
    }
  }
}
