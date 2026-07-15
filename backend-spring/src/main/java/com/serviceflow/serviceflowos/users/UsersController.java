package com.serviceflow.serviceflowos.users;

import com.serviceflow.serviceflowos.domain.User;
import com.serviceflow.serviceflowos.domain.UserRepository;
import com.serviceflow.serviceflowos.security.TenantContext;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates the tenant-isolation pattern every future feature follows: never query
 * "all users," always scope to TenantContext.get() so one tenant can never see another
 * tenant's rows, no matter what a caller sends in a request.
 */
@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserRepository userRepository;

    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Map<String, Object>> listUsersInCurrentTenant() {
        return userRepository.findByTenantId(TenantContext.get()).stream()
                .map(this::toSummary)
                .toList();
    }

    private Map<String, Object> toSummary(User user) {
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole());
    }
}
