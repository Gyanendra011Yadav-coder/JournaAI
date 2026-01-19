package ai.journa.prcontrol.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HtmlTextExtractor {
  private static final String[] STRIP_SELECTORS = {
      "script", "style", "noscript", "svg", "iframe", "header", "footer", "nav",
      "aside", "form", "button", "input", "select", "option", "textarea", "dialog"
  };
  private static final Set<String> NOISE_MARKERS = new HashSet<>(Arrays.asList(
      "nav", "menu", "footer", "header", "sidebar", "subscribe", "newsletter",
      "cookie", "consent", "popup", "modal", "share", "social", "related",
      "trending", "comment", "promo", "advert", "ads", "banner", "login",
      "signup", "register", "breadcrumb", "pager", "pagination", "tag",
      "widget", "toolbar", "topbar", "bottombar", "search", "filter", "language",
      "edition"
  ));

  private HtmlTextExtractor() {
  }

  public static String extractMainText(Document document) {
    if (document == null) {
      return "";
    }
    Document clone = document.clone();
    for (String selector : STRIP_SELECTORS) {
      clone.select(selector).remove();
    }
    for (Element element : clone.select("[class], [id]")) {
      String marker = (element.className() + " " + element.id()).toLowerCase(Locale.ROOT);
      if (isNoiseMarker(marker)) {
        element.remove();
      }
    }
    String preferred = extractPreferredBlocks(clone);
    if (preferred != null && !preferred.isBlank()) {
      return preferred.trim();
    }
    Element body = clone.body();
    return body != null ? body.text().trim() : clone.text().trim();
  }

  private static boolean isNoiseMarker(String marker) {
    if (marker == null || marker.isBlank()) {
      return false;
    }
    for (String token : NOISE_MARKERS) {
      if (marker.contains(token)) {
        return true;
      }
    }
    return false;
  }

  private static String extractPreferredBlocks(Document document) {
    Elements candidates = new Elements();
    Element main = document.selectFirst("main");
    if (main != null) {
      candidates.add(main);
    }
    Element article = document.selectFirst("article");
    if (article != null) {
      candidates.add(article);
    }
    candidates.addAll(document.select(
        "[itemprop=author], [class*=author], [class*=byline], [class*=profile], [class*=bio], [id*=author], [id*=profile]"
    ));
    StringBuilder builder = new StringBuilder();
    Set<String> seen = new HashSet<>();
    for (Element candidate : candidates) {
      String text = candidate.text();
      if (text == null || text.isBlank()) {
        continue;
      }
      String normalized = text.trim();
      if (normalized.length() < 80 || seen.contains(normalized)) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append('\n');
      }
      builder.append(normalized);
      seen.add(normalized);
      if (builder.length() > 1500) {
        break;
      }
    }
    return builder.toString();
  }
}
