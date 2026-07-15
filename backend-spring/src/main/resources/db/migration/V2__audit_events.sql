create table audit_events (
    id uuid primary key,
    tenant_id uuid references tenants(id),
    actor_user_id uuid references users(id),
    action varchar(100) not null,
    metadata text,
    created_at timestamptz not null default now()
);

create index idx_audit_events_tenant_id on audit_events(tenant_id);
create index idx_audit_events_created_at on audit_events(created_at);
