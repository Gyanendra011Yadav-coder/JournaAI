package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.LoginRequest;
import ai.journa.prcontrol.dto.LoginResponse;
import ai.journa.prcontrol.dto.MeResponse;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.service.AuthService;
import ai.journa.prcontrol.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;
  private final CurrentUserService currentUserService;

  public AuthController(AuthService authService, CurrentUserService currentUserService) {
    this.authService = authService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    AuthService.LoginResult result = authService.login(request);
    User user = result.user();
    return new LoginResponse(result.token(), user.getRole().name(), user.getEmail());
  }

  @PostMapping("/register")
  public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
    AuthService.LoginResult result = authService.register(request);
    User user = result.user();
    return new LoginResponse(result.token(), user.getRole().name(), user.getEmail());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public MeResponse me() {
    User user = currentUserService.requireCurrentUser();
    return new MeResponse(user.getEmail(), user.getRole().name());
  }
}
