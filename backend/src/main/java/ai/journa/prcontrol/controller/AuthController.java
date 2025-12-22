package ai.journa.prcontrol.controller;

import ai.journa.prcontrol.dto.LoginRequest;
import ai.journa.prcontrol.dto.LoginResponse;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(new LoginResponse(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new LoginResponse(authService.login(request)));
    }
}
