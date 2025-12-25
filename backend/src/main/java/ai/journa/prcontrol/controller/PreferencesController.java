package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.ProfileResponse;
import ai.journa.prcontrol.dto.ProfileUpdateRequest;
import ai.journa.prcontrol.service.CurrentUserService;
import ai.journa.prcontrol.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {
  private final ProfileService profileService;
  private final CurrentUserService currentUserService;

  public PreferencesController(ProfileService profileService, CurrentUserService currentUserService) {
    this.profileService = profileService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public ProfileResponse getPreferences() {
    return profileService.getProfile(currentUserService.requireCurrentUser());
  }

  @PutMapping
  public ProfileResponse updatePreferences(@Valid @RequestBody ProfileUpdateRequest request) {
    return profileService.updateProfile(currentUserService.requireCurrentUser(), request);
  }
}
