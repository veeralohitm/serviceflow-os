create table tenants (
    id uuid primary key,
    name varchar(255) not null,
    status varchar(50) not null default 'active',
    created_at timestamptz not null default now()
);

create table users (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(50) not null,
    status varchar(50) not null default 'active',
    created_at timestamptz not null default now()
);

create index idx_users_tenant_id on users(tenant_id);
