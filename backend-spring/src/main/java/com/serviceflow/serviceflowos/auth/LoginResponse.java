package com.serviceflow.serviceflowos.auth;

public record LoginResponse(
        String token,
        String userId,
        String email,
        String role,
        String tenantId) {
}
