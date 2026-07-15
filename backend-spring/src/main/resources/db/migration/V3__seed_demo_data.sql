-- Dev/demo seed data only. Two tenants so tenant isolation is provable,
-- not just theoretical. Both admins share the password 'admin123' for
-- convenience in local testing; the hash below is a real BCrypt hash of
-- that password, generated with the same BCryptPasswordEncoder the app uses.

insert into tenants (id, name, status, created_at) values
    ('11111111-1111-1111-1111-111111111111', 'Demo HVAC Co', 'active', now()),
    ('22222222-2222-2222-2222-222222222222', 'Acme Plumbing', 'active', now());

insert into users (id, tenant_id, email, password_hash, role, status, created_at) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111',
     'admin@demo.serviceflow.os', '$2a$10$V/fJRP1lSRV2jpGgtKqJH.7j8EIQyxAIw.2S6LPcPSjehH5s5iW0u',
     'ADMIN', 'active', now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222',
     'admin@acme.serviceflow.os', '$2a$10$V/fJRP1lSRV2jpGgtKqJH.7j8EIQyxAIw.2S6LPcPSjehH5s5iW0u',
     'ADMIN', 'active', now());
