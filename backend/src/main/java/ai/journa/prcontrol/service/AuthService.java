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
  private final ProfileService profileService;
  private final ClientService clientService;

  public AuthService(UserRepository userRepository,
                     PasswordEncoder passwordEncoder,
                     JwtService jwtService,
                     ProfileService profileService,
                     ClientService clientService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.profileService = profileService;
    this.clientService = clientService;
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
    user.setRole(Role.MEMBER);
    userRepository.save(user);
    profileService.createProfileFromRegister(user, request);
    clientService.createFromRegister(user, request);
    String token = jwtService.generateToken(user.getEmail(), user.getRole());
    return new LoginResult(token, user);
  }

  public record LoginResult(String token, User user) {
  }
}
