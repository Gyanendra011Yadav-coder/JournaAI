package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Role;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.dto.LoginRequest;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.exception.EmailAlreadyRegisteredException;
import ai.journa.prcontrol.repository.UserRepository;
import ai.journa.prcontrol.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository,
                     PasswordEncoder passwordEncoder,
                     JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public LoginResult login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    String token = jwtService.generateToken(user.getEmail(), user.getRole());
    return new LoginResult(token, user);
  }

  public LoginResult register(RegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new EmailAlreadyRegisteredException(request.getEmail());
    }
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.ADMIN);
    userRepository.save(user);
    String token = jwtService.generateToken(user.getEmail(), user.getRole());
    return new LoginResult(token, user);
  }

  public record LoginResult(String token, User user) {
  }
}
