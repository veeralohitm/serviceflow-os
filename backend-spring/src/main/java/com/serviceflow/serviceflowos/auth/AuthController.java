package com.serviceflow.serviceflowos.auth;

import com.serviceflow.serviceflowos.domain.AuditEvent;
import com.serviceflow.serviceflowos.domain.AuditEventRepository;
import com.serviceflow.serviceflowos.domain.User;
import com.serviceflow.serviceflowos.domain.UserRepository;
import com.serviceflow.serviceflowos.security.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditEventRepository auditEventRepository;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditEventRepository auditEventRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditEventRepository = auditEventRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        boolean valid = userOpt.isPresent()
                && passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash());

        if (!valid) {
            auditEventRepository.save(new AuditEvent(
                    userOpt.map(User::getTenant).orElse(null),
                    userOpt.orElse(null),
                    "LOGIN_FAILURE",
                    "email attempted: " + request.email()));
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user);

        auditEventRepository.save(new AuditEvent(user.getTenant(), user, "LOGIN_SUCCESS", null));

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenant().getId().toString()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "tenantId", user.getTenant().getId()));
    }
}
