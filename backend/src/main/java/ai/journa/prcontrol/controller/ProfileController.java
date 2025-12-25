package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.ProfileResponse;
import ai.journa.prcontrol.dto.ProfileUpdateRequest;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class ProfileController {
  private final ProfileService profileService;
  private final CurrentUserService currentUserService;

  public ProfileController(ProfileService profileService, CurrentUserService currentUserService) {
    this.profileService = profileService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/profile")
  public ProfileResponse getProfile() {
    return profileService.getProfile(currentUserService.requireCurrentUser());
  }

  @PutMapping("/profile")
  public ProfileResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
    return profileService.updateProfile(currentUserService.requireCurrentUser(), request);
  }

  @GetMapping("/preferences")
  public ProfileResponse getPreferences() {
    return getProfile();
  }

  @PutMapping("/preferences")
  public ProfileResponse updatePreferences(@Valid @RequestBody ProfileUpdateRequest request) {
    return updateProfile(request);
  }
}
