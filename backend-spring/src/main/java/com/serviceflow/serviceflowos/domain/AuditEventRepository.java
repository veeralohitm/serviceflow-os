package com.serviceflow.serviceflowos.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
