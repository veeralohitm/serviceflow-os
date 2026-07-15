package com.serviceflow.serviceflowos.security;

import com.serviceflow.serviceflowos.domain.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once per request: reads the "Authorization: Bearer <token>" header, and if the
 * token is a valid JWT for a known user, marks the request as authenticated. No token
 * or an invalid one just means the request continues as anonymous - it's on the
 * security config to decide which endpoints require authentication at all.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtService.isValid(token)) {
                    Claims claims = jwtService.extractClaims(token);
                    userRepository.findByEmail(claims.getSubject()).ifPresent(user -> {
                        List<SimpleGrantedAuthority> authorities =
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        TenantContext.set(user.getTenant().getId());
                    });
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
