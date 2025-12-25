package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.IntegrationSettings;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.domain.UserProfile;
import ai.journa.prcontrol.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
public class LocaleResolver {
  private static final String[] COUNTRY_HEADERS = {"CF-IPCountry", "X-Country-Code"};

  private final IntegrationSettingsService integrationSettingsService;
  private final UserProfileRepository userProfileRepository;

  public LocaleResolver(IntegrationSettingsService integrationSettingsService, UserProfileRepository userProfileRepository) {
    this.integrationSettingsService = integrationSettingsService;
    this.userProfileRepository = userProfileRepository;
  }

  public Resolution resolve(User user, HttpServletRequest request) {
    UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    String[] preferredCountries = profile != null ? profile.getPreferredCountries() : null;
    String[] preferredLangs = profile != null ? profile.getPreferredLangs() : null;

    String resolvedCountry = resolveCountry(request, preferredCountries, settings.getDefaultCountry());
    String resolvedLang = resolveLanguage(request, preferredLangs, settings.getDefaultLang());

    return new Resolution(resolvedCountry, resolvedLang, preferredCountries, preferredLangs);
  }

  public Resolution resolveDefaults() {
    IntegrationSettings settings = integrationSettingsService.getActiveSettings();
    String country = settings.getDefaultCountry() != null ? settings.getDefaultCountry().toLowerCase(Locale.ROOT) : null;
    String lang = settings.getDefaultLang() != null ? settings.getDefaultLang().toLowerCase(Locale.ROOT) : null;
    return new Resolution(country, lang, null, null);
  }

  private String resolveCountry(HttpServletRequest request, String[] preferredCountries, String defaultCountry) {
    String headerCountry = null;
    if (request != null) {
      for (String header : COUNTRY_HEADERS) {
        String value = request.getHeader(header);
        if (value != null && !value.isBlank()) {
          headerCountry = value.trim().toLowerCase(Locale.ROOT);
          break;
        }
      }
    }
    String candidate = headerCountry != null ? headerCountry : first(preferredCountries).orElse(null);
    if (candidate == null || candidate.isBlank()) {
      candidate = defaultCountry;
    }
    if (preferredCountries != null && preferredCountries.length > 0) {
      String normalized = candidate != null ? candidate.toLowerCase(Locale.ROOT) : null;
      boolean allowed = Arrays.stream(preferredCountries)
          .filter(value -> value != null && !value.isBlank())
          .anyMatch(value -> value.trim().equalsIgnoreCase(normalized));
      if (!allowed) {
        return preferredCountries[0].toLowerCase(Locale.ROOT);
      }
    }
    return candidate != null ? candidate.toLowerCase(Locale.ROOT) : null;
  }

  private String resolveLanguage(HttpServletRequest request, String[] preferredLangs, String defaultLang) {
    String candidate = first(preferredLangs).orElse(null);
    if (candidate == null && request != null) {
      String accept = request.getHeader("Accept-Language");
      if (accept != null && !accept.isBlank()) {
        String tag = accept.split(",")[0];
        if (tag != null && !tag.isBlank()) {
          candidate = Locale.forLanguageTag(tag.trim()).getLanguage();
        }
      }
    }
    if (candidate == null || candidate.isBlank()) {
      candidate = defaultLang;
    }
    if (preferredLangs != null && preferredLangs.length > 0) {
      String normalized = candidate != null ? candidate.toLowerCase(Locale.ROOT) : null;
      boolean allowed = Arrays.stream(preferredLangs)
          .filter(value -> value != null && !value.isBlank())
          .anyMatch(value -> value.trim().equalsIgnoreCase(normalized));
      if (!allowed) {
        return preferredLangs[0].toLowerCase(Locale.ROOT);
      }
    }
    return candidate != null ? candidate.toLowerCase(Locale.ROOT) : null;
  }

  private Optional<String> first(String[] values) {
    if (values == null || values.length == 0) {
      return Optional.empty();
    }
    String first = values[0];
    return first == null || first.isBlank() ? Optional.empty() : Optional.of(first.trim());
  }

  public record Resolution(String country, String lang, String[] preferredCountries, String[] preferredLangs) {}
}
