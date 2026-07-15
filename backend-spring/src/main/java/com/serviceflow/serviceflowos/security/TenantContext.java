package com.serviceflow.serviceflowos.security;

import java.util.UUID;

/**
 * Holds "which tenant is this request acting as" for the duration of one request.
 * Set by JwtAuthFilter right after a token is verified, read by anything that needs
 * to scope a query to the caller's tenant, cleared at the end of the request so it
 * never leaks into a reused thread.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
