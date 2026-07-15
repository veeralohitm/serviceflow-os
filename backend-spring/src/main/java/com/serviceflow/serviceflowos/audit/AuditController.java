package com.serviceflow.serviceflowos.audit;

import com.serviceflow.serviceflowos.domain.AuditEvent;
import com.serviceflow.serviceflowos.domain.AuditEventRepository;
import com.serviceflow.serviceflowos.security.TenantContext;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public List<Map<String, Object>> recentEventsForCurrentTenant() {
        return auditEventRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.get()).stream()
                .map(this::toSummary)
                .toList();
    }

    private Map<String, Object> toSummary(AuditEvent event) {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("action", event.getAction());
        summary.put("actorEmail", event.getActorUser() != null ? event.getActorUser().getEmail() : null);
        summary.put("metadata", event.getMetadata());
        summary.put("createdAt", event.getCreatedAt());
        return summary;
    }
}
