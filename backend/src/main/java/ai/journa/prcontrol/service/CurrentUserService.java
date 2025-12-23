package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User requireCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("User not authenticated");
    }
    return userRepository.findByEmail(auth.getName())
        .orElseThrow(() -> new IllegalStateException("User not found"));
  }
}
