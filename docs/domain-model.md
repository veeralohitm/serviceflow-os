# ServiceFlow OS — Domain Model (v1, Checkpoint 1 / Day 1)

Derived from the entity list and functional requirements in [`prd.md`](./prd.md). This is the schema we build toward across the roadmap — later checkpoints add fields as each module gets built (e.g. `Job` gains invoicing fields at Checkpoint 10), but the entities and relationships below shouldn't need to change shape.

Every table except `Role` and seed/reference data carries a `tenant_id` — this is a multi-tenant system, and tenant isolation is called out in the PRD as an early, non-negotiable concern.

## Identity & tenancy

**Tenant**
`id, name, status (active/suspended), created_at`

**Role** — fixed reference data, not tenant-scoped: `Admin, CSR, Dispatcher, Technician, Manager`
`id, name`

**User** — internal staff only for MVP. Customers don't log in until the post-MVP "customer portal."
`id, tenant_id, employee_id (nullable), email, password_hash, role_id, status (invited/active/disabled), created_at`

## Staff & operations

**Employee**
`id, tenant_id, user_id (nullable), first_name, last_name, phone, email, employment_status, hire_date`

**Skill**
`id, tenant_id, name`

**EmployeeSkill** (join table — many-to-many, implied by "skills" being plural per employee)
`employee_id, skill_id`

**Territory**
`id, tenant_id, name, zip_codes`

**EmployeeTerritory** (join table)
`employee_id, territory_id`

**Shift**
`id, tenant_id, employee_id, date, start_time, end_time, is_pto`

## Customer & location

**Customer**
`id, tenant_id, display_name, lead_source, referral_source, tags, created_at`

**Contact** — one customer, multiple contacts
`id, customer_id, first_name, last_name, phone, email, is_primary, contact_preferences`

**Location** — one customer, multiple service addresses
`id, customer_id, territory_id, address_line1, address_line2, city, state, zip, notes`

**Equipment**
`id, location_id, type, brand, model, install_date, notes`

**MembershipPlan**
`id, tenant_id, name, price, billing_frequency, included_services, visit_entitlements`

**CustomerMembership**
`id, customer_id, plan_id, location_id (nullable), start_date, renewal_date, status`

## Booking & jobs

**Lead**
`id, tenant_id, customer_id (nullable — may predate a customer record), source, description, status, created_at`

**Job** — the central entity; status values per PRD: `new, booked, dispatched, on_the_way, in_progress, completed, invoiced, paid, canceled`
`id, tenant_id, customer_id, location_id, lead_id (nullable), job_type, description, status, priority, created_by, created_at`

**Appointment**
`id, job_id, scheduled_start, scheduled_end, arrival_window_start (nullable), arrival_window_end (nullable), buffer_before, buffer_after, technician_id (nullable = unassigned queue)`

**DispatchAssignment** — kept separate from Appointment so reassignment has a history trail (dispatcher rebalancing is a named PRD workflow)
`id, appointment_id, employee_id, assigned_at, assigned_by, status (assigned/acknowledged/en_route/completed)`

## Pricing & billing

**PricebookItem**
`id, tenant_id, name, category, type (service/material), unit_price, taxable, bundle_id (nullable, self-referencing)`

**Estimate**
`id, job_id, status (draft/sent/approved/declined), subtotal, tax_total, discount_total, total, created_by, created_at`

**EstimateLine**
`id, estimate_id, pricebook_item_id, quantity, unit_price, line_total`

**Invoice**
`id, job_id, estimate_id (nullable), status (draft/sent/partial/paid/void), subtotal, tax_total, discount_total, total, balance_due, due_date, created_at`

**InvoiceLine**
`id, invoice_id, pricebook_item_id, quantity, unit_price, line_total`

**Payment**
`id, invoice_id, amount, method (cash/check/card-manual), paid_at, recorded_by`

> Money fields are `BigDecimal`/`numeric`, never `float` or `double` — this is a Day 18 (Checkpoint 9) reminder worth locking in now.

## Cross-cutting

**Note** — polymorphic: attaches to a customer, job, invoice, etc.
`id, tenant_id, related_entity_type, related_entity_id, author_id, body, created_at`

**Attachment** — same polymorphic pattern, for photos/forms/signatures
`id, tenant_id, related_entity_type, related_entity_id, file_url, file_name, uploaded_by, uploaded_at`

**Notification**
`id, tenant_id, type (booking_confirmation/reminder/on_the_way/estimate_approval/payment_reminder/review_request), recipient_contact_id, channel, status, sent_at`

**AuditEvent**
`id, tenant_id, actor_user_id, action, metadata, created_at` — `entity_type`/`entity_id` dropped from the original sketch; Checkpoint 2 only logs auth events (`LOGIN_SUCCESS`/`LOGIN_FAILURE`), which have no entity to point at. Re-add if a future checkpoint audits entity-level changes (e.g. "who edited this invoice").

## Relationship summary

```
Tenant 1───* everything (multi-tenant root)

Customer 1───* Contact
Customer 1───* Location
Customer 1───* CustomerMembership
Location 1───* Equipment
Location 1───* Job

Job 1───* Appointment
Job 1───* Estimate
Job 1───* Invoice
Job 1───* Note / Attachment

Appointment 1───* DispatchAssignment
Employee *───* Skill        (via EmployeeSkill)
Employee *───* Territory    (via EmployeeTerritory)
Employee 1───* Shift
Employee 1───* Appointment  (as assigned technician)

Estimate 1───* EstimateLine ───> PricebookItem
Invoice  1───* InvoiceLine  ───> PricebookItem
Invoice  1───* Payment

MembershipPlan 1───* CustomerMembership
```

## MVP scope decisions (Day 1)

Per the PRD's non-goals, explicitly **out** for now:
- Payroll / general ledger accounting
- Native telephony
- Marketing attribution
- Multi-day construction project management
- Third-party contractor marketplace
- Card payment processing (Payment records *manual* payments only — no PCI scope)
- Customer login/portal (Users are internal staff only)

Simplifications from the raw PRD entity list, to revisit post-MVP:
- `Role` is fixed reference data (5 rows), not a user-editable permission system. **Implemented (Day 3)** as a plain Java `enum`, not a database table — five code-defined values don't earn a table + FK until roles become user-editable, which the PRD doesn't ask for.
- `DispatchAssignment` doubles as both "current assignment" and reassignment history — no separate audit table needed yet; `AuditEvent` covers auth-sensitive actions only, per PRD Epic 2. **Implemented (Day 4)**: `AuditEvent` logs `LOGIN_SUCCESS`/`LOGIN_FAILURE` only so far.

## Open questions to confirm before Checkpoint 2 (auth)

- [x] Can one `Employee` hold more than one `Role`? **Resolved (Day 3): no** — `User.role` is a single enum column, one role per user for MVP.
- [ ] Does `Job` belong to a `Location` directly, or only reachable via `Customer` → `Location`? Modeled above as a direct FK on `Job` for query simplicity. Still open — not yet built (Checkpoint 5).

## Checkpoint 2 implementation notes (Days 3–4)

- **Tenant isolation pattern**: `JwtAuthFilter` reads `tenantId` out of the JWT claims and sets it on a request-scoped `TenantContext` (a cleared-per-request `ThreadLocal`). Every tenant-scoped query — `GET /api/users`, `GET /api/audit` so far — reads `TenantContext.get()` rather than trusting anything the client sends. This is the pattern every future feature (customers, jobs, invoices...) should follow: never query "all rows," always scope to the current tenant from the token, not from a request parameter a client could tamper with.
- **Schema ownership**: Flyway migrations (`db/migration/V1__init_schema.sql`, `V2__audit_events.sql`, `V3__seed_demo_data.sql`) are now the source of truth for the database shape; `spring.jpa.hibernate.ddl-auto` is `validate`, not `update` — Hibernate checks the schema matches the entities but never changes it.
- **Seed data**: two demo tenants ("Demo HVAC Co," "Acme Plumbing") each with one admin user, seeded via `V3__seed_demo_data.sql` rather than the temporary `DevDataSeeder` (removed Day 4). Real password hash generated once with the project's own `BCryptPasswordEncoder`, not a different tool's hash format.
