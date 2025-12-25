package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Beat;
import ai.journa.prcontrol.domain.SidebarMode;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.domain.UserKeyword;
import ai.journa.prcontrol.domain.UserKeywordKind;
import ai.journa.prcontrol.domain.UserProfile;
import ai.journa.prcontrol.dto.ProfileResponse;
import ai.journa.prcontrol.dto.ProfileUpdateRequest;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.repository.BeatRepository;
import ai.journa.prcontrol.repository.UserKeywordRepository;
import ai.journa.prcontrol.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProfileService {
  private final UserProfileRepository userProfileRepository;
  private final UserKeywordRepository userKeywordRepository;
  private final BeatRepository beatRepository;

  public ProfileService(UserProfileRepository userProfileRepository,
                        UserKeywordRepository userKeywordRepository,
                        BeatRepository beatRepository) {
    this.userProfileRepository = userProfileRepository;
    this.userKeywordRepository = userKeywordRepository;
    this.beatRepository = beatRepository;
  }

  @Transactional
  public ProfileResponse getProfile(User user) {
    UserProfile profile = ensureProfile(user);
    List<UserKeyword> keywords = userKeywordRepository.findByUser_Id(user.getId());
    return toResponse(user, profile, keywords);
  }

  @Transactional
  public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
    UserProfile profile = ensureProfile(user);
    if (request.getPreferredCountries() != null) {
      profile.setPreferredCountries(request.getPreferredCountries().toArray(new String[0]));
    }
    if (request.getPreferredLangs() != null) {
      profile.setPreferredLangs(request.getPreferredLangs().toArray(new String[0]));
    }
    if (request.getDefaultSidebarMode() != null) {
      profile.setDefaultSidebarMode(SidebarMode.valueOf(request.getDefaultSidebarMode().toUpperCase(Locale.ROOT)));
    }
    if (request.getClientLensRatio() != null) {
      profile.setClientLensRatio(request.getClientLensRatio());
    }
    if (request.getTrendingLocalRatio() != null) {
      profile.setTrendingLocalRatio(request.getTrendingLocalRatio());
    }
    if (request.getBeatIds() != null) {
      Set<Beat> beats = new HashSet<>(beatRepository.findAllById(request.getBeatIds()));
      profile.setBeats(beats);
    }
    profile.setUpdatedAt(Instant.now());
    userProfileRepository.save(profile);

    if (request.getClientKeywords() != null || request.getExcludeKeywords() != null) {
      userKeywordRepository.deleteAll(userKeywordRepository.findByUser_Id(user.getId()));
      saveKeywords(user, request.getClientKeywords(), UserKeywordKind.CLIENT);
      saveKeywords(user, request.getExcludeKeywords(), UserKeywordKind.EXCLUDE);
    }

    List<UserKeyword> keywords = userKeywordRepository.findByUser_Id(user.getId());
    return toResponse(user, profile, keywords);
  }

  @Transactional
  public void createProfileFromRegister(User user, RegisterRequest request) {
    UserProfile profile = ensureProfile(user);
    if (request.getPreferredCountries() != null) {
      profile.setPreferredCountries(request.getPreferredCountries().toArray(new String[0]));
    }
    if (request.getPreferredLangs() != null) {
      profile.setPreferredLangs(request.getPreferredLangs().toArray(new String[0]));
    }
    if (request.getBeatIds() != null && !request.getBeatIds().isEmpty()) {
      profile.setBeats(new HashSet<>(beatRepository.findAllById(request.getBeatIds())));
    }
    profile.setUpdatedAt(Instant.now());
    userProfileRepository.save(profile);

    saveKeywords(user, request.getClientKeywords(), UserKeywordKind.CLIENT);
    saveKeywords(user, request.getExcludeKeywords(), UserKeywordKind.EXCLUDE);
  }

  private UserProfile ensureProfile(User user) {
    return userProfileRepository.findById(user.getId())
        .orElseGet(() -> {
          UserProfile profile = new UserProfile();
          profile.setUser(user);
          profile.setUserId(user.getId());
          profile.setUpdatedAt(Instant.now());
          return userProfileRepository.save(profile);
        });
  }

  private void saveKeywords(User user, List<String> keywords, UserKeywordKind kind) {
    if (keywords == null) {
      return;
    }
    for (String keyword : keywords) {
      if (keyword == null || keyword.isBlank()) {
        continue;
      }
      UserKeyword entity = new UserKeyword();
      entity.setUser(user);
      entity.setKind(kind);
      entity.setKeyword(keyword.trim());
      userKeywordRepository.save(entity);
    }
  }

  private ProfileResponse toResponse(User user, UserProfile profile, List<UserKeyword> keywords) {
    ProfileResponse response = new ProfileResponse();
    response.setEmail(user.getEmail());
    response.setPreferredCountries(profile.getPreferredCountries() != null ? List.of(profile.getPreferredCountries()) : List.of());
    response.setPreferredLangs(profile.getPreferredLangs() != null ? List.of(profile.getPreferredLangs()) : List.of());
    response.setBeatIds(profile.getBeats().stream().map(Beat::getId).toList());
    response.setClientKeywords(keywords.stream()
        .filter(keyword -> keyword.getKind() == UserKeywordKind.CLIENT)
        .map(UserKeyword::getKeyword)
        .toList());
    response.setExcludeKeywords(keywords.stream()
        .filter(keyword -> keyword.getKind() == UserKeywordKind.EXCLUDE)
        .map(UserKeyword::getKeyword)
        .toList());
    response.setDefaultSidebarMode(profile.getDefaultSidebarMode() != null ? profile.getDefaultSidebarMode().name() : null);
    response.setClientLensRatio(profile.getClientLensRatio());
    response.setTrendingLocalRatio(profile.getTrendingLocalRatio());
    return response;
  }
}
