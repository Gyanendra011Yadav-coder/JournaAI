package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.Role;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.domain.Workspace;
import ai.journa.prcontrol.dto.LoginRequest;
import ai.journa.prcontrol.dto.RegisterRequest;
import ai.journa.prcontrol.repository.UserRepository;
import ai.journa.prcontrol.repository.WorkspaceRepository;
import ai.journa.prcontrol.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }
        Workspace workspace = new Workspace();
        workspace.setName(request.getWorkspaceName());
        workspaceRepository.save(workspace);

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        Role role = userRepository.count() == 0 ? Role.ADMIN : Role.MEMBER;
        user.setRole(role);
        user.setWorkspace(workspace);
        userRepository.save(user);
        return jwtService.generateToken(user.getEmail(), user.getRole());
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalStateException("Invalid credentials");
        }
        return jwtService.generateToken(user.getEmail(), user.getRole());
    }
}
