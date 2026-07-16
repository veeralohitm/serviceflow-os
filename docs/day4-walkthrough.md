# Day 4 Walkthrough — Checkpoint 2, Day 4: Migrations, Tenant Isolation, Audit Trail

Hand this doc to someone else along with the Day 1–3 walkthroughs and they can complete Day 4 entirely on their own. It assumes Day 3 (JWT auth end to end, login UI) is already done.

## What Day 4 actually produces

By the end of today:
1. The database schema is owned by **versioned migration files**, not by Hibernate guessing at changes.
2. **Two separate tenants** exist, each with their own admin user, and you can prove neither can see the other's data.
3. A basic **audit trail** records every login attempt, success or failure.

Budget **4–5 hours**. This day includes one genuinely confusing real-world debugging detour (Step 2) — expect it, don't panic when a dependency you added seems to do nothing.

## Before you start

- Day 3 done: JWT login working end to end, one seeded admin user via a temporary `CommandLineRunner`.
- Comfortable with basic SQL (`create table`, `insert`) from Day 1's domain modeling.

## Step 1 — Learn the concept: migrations vs. `ddl-auto`

Since Day 2, `spring.jpa.hibernate.ddl-auto=update` has let Hibernate silently reshape the database to match your Java entities on every startup. Convenient for a fresh project, dangerous once real data exists — Hibernate's guess at "how to change the schema" isn't always safe (it might not know how to rename a column without dropping data, for instance) and it isn't reviewable by anyone before it runs.

A **migration** is a small SQL file, numbered in order (`V1__`, `V2__`, `V3__`...), that a tool called Flyway runs exactly once per database, tracking what's already been applied in its own history table. The schema's history becomes reviewable the same way your code's is: anyone can read the migration files, in order, and see exactly how the database got to its current shape.

## Step 2 — Add Flyway, and a real gotcha worth understanding

The obvious first attempt is adding `flyway-core` plus `flyway-database-postgresql` (Postgres needs a database-specific Flyway module in addition to the core library) to `pom.xml`. Do this, restart the app, and... nothing happens. No error, no Flyway log banner, straight past it into Hibernate — which then fails because the tables the migrations were supposed to create don't exist.

**Don't guess at this — diagnose it.** Two commands cut straight to the answer instead of trial-and-error:

```bash
# Does the dependency even resolve?
./mvnw dependency:tree | grep -i flyway

# Does the autoconfiguration jar actually contain a Flyway autoconfiguration class?
find ~/.m2 -iname "spring-boot-autoconfigure-*.jar" | head -1 | xargs -I{} unzip -l {} | grep -i flyway
```

The first command shows `flyway-core` resolved fine. The second comes back **empty** — the autoconfiguration jar has zero Flyway-related classes in it. That's the real answer: **Spring Boot 4 split Flyway's autoconfiguration into its own dedicated starter**, separate from the `flyway-core` library itself, rather than bundling every optional integration into one monolithic `spring-boot-autoconfigure` jar (the same pattern shows up elsewhere in Boot 4 — `spring-boot-starter-webmvc` instead of the old `spring-boot-starter-web`, for instance). Confirm the exact artifact name by checking Spring Boot's own dependency-management BOM rather than guessing at a name:

```bash
grep -B3 "spring-boot-starter-flyway" \
  ~/.m2/repository/org/springframework/boot/spring-boot-dependencies/*/spring-boot-dependencies-*.pom
```

The fix, in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**The lesson, generalized:** when a dependency you added produces no error but also visibly does nothing, don't keep tweaking config blindly — check whether the classes you're depending on for autoconfiguration to work are actually on the classpath at all. `unzip -l` on a jar is a completely reliable way to answer "is this class really here," faster than any amount of Stack Overflow searching.

## Step 3 — Let Flyway own the schema

In `application.properties`, change:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

`validate` means Hibernate checks on startup that your entities match the actual schema, and fails loudly if they don't — but it never modifies the schema itself. Flyway is now the only thing allowed to change table shape.

## Step 4 — Write the schema migration

Create `backend-spring/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
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
```

This mirrors the `Tenant`/`User` JPA entities from Day 3 exactly — column names, nullability, the foreign key. No `id` default is needed because the Java entities use `@GeneratedValue(strategy = GenerationType.UUID)`, meaning the ID is generated in application code, not by the database.

## Step 5 — Write the audit table migration

`V2__audit_events.sql`:

```sql
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
```

`tenant_id` and `actor_user_id` are nullable here, unlike `users.tenant_id` — a failed login with an email that matches no user has no tenant and no user to attach the event to, but the attempt still needs recording.

## Step 6 — Generate a real password hash for seed data

The seed migration needs a working password hash, but you can't run Java code from inside a `.sql` file. Rather than trust a different tool's bcrypt variant, generate the hash with the exact same `BCryptPasswordEncoder` class the app already uses, as a tiny standalone script:

```bash
mkdir -p /tmp/hashgen && cd /tmp/hashgen
cat > HashGen.java <<'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashGen {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
EOF

JAR=$(find ~/.m2 -iname "spring-security-crypto-*.jar" | head -1)
CL=$(find ~/.m2 -iname "commons-logging-*.jar" | head -1)
javac -cp "$JAR" HashGen.java
java -cp ".:$JAR:$CL" HashGen admin123
```

This prints a real `$2a$...` hash. `commons-logging` is needed too, even though you'd only expect to need the security-crypto jar — `BCryptPasswordEncoder`'s constructor logs a message, and that pulls in a logging dependency transitively. If you hit a `NoClassDefFoundError` for some unrelated-looking class, it's usually exactly this: a transitive dependency the class you're calling needs but doesn't ship bundled.

## Step 7 — Write the seed migration

`V3__seed_demo_data.sql` — **two tenants**, deliberately, not one. A single tenant can't prove isolation; you need a second tenant's data to confirm the first one can't see it:

```sql
insert into tenants (id, name, status, created_at) values
    ('11111111-1111-1111-1111-111111111111', 'Demo HVAC Co', 'active', now()),
    ('22222222-2222-2222-2222-222222222222', 'Acme Plumbing', 'active', now());

insert into users (id, tenant_id, email, password_hash, role, status, created_at) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111',
     'admin@demo.serviceflow.os', '<paste hash from Step 6>', 'ADMIN', 'active', now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222',
     'admin@acme.serviceflow.os', '<same hash>', 'ADMIN', 'active', now());
```

Fixed, readable UUIDs (not random ones) are a deliberate choice for seed data — they're easy to recognize and reference later in tests or docs.

## Step 8 — Remove the temporary seeder

Delete `DevDataSeeder.java` from Day 3. Its entire job — getting one user into the database to log in as — is now done properly by `V3__seed_demo_data.sql`, and having both around would just seed data twice.

## Step 9 — Reset the database and verify

Since the previous Hibernate-managed schema doesn't match what Flyway expects to create, wipe the local dev database and let Flyway build it from scratch — safe, because this is disposable local dev data, not anything real:

```bash
docker compose down -v
docker compose up -d postgres
cd backend-spring && ./mvnw spring-boot:run
```

Confirm all three migrations actually ran:

```bash
docker exec -i <postgres-container-name> psql -U serviceflow -d serviceflow \
  -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

You should see three rows, all `success = t`.

## Step 10 — Build `TenantContext`

This is the actual tenant-isolation mechanism. A `ThreadLocal` holds "which tenant is this request acting as," for the duration of one request only:

```java
public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void set(UUID tenantId) { CURRENT_TENANT.set(tenantId); }
    public static UUID get() { return CURRENT_TENANT.get(); }
    public static void clear() { CURRENT_TENANT.remove(); }
}
```

**Why a `ThreadLocal` and not, say, a request parameter:** trusting a tenant ID that the *client* sends is a security bug waiting to happen — nothing stops a malicious or buggy client from sending someone else's tenant ID. The tenant ID has to come from something the server itself verified: the JWT's `tenantId` claim, already checked for a valid signature by Day 3's `JwtAuthFilter`.

## Step 11 — Populate and clear it in the JWT filter

In `JwtAuthFilter`, right after resolving the authenticated user, set the tenant context — and wrap the whole thing in a `try`/`finally` so it's always cleared, even if something downstream throws:

```java
try {
    if (header != null && header.startsWith("Bearer ")) {
        // ...validate token, resolve user...
        TenantContext.set(user.getTenant().getId());
    }
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();
}
```

**Why clearing matters, specifically:** the servlet container reuses a fixed pool of threads across many requests. Without `clear()`, a `ThreadLocal` value set during one request can leak into the *next*, unrelated request that happens to run on the same thread — a subtle, hard-to-reproduce bug where one user occasionally sees another user's data for no apparent reason. Always pair `set()` with a guaranteed `clear()`.

## Step 12 — Build the audit trail

`AuditEvent` entity — `id`, an optional `Tenant`, an optional `User` (the actor), an `action` string, `metadata`, `createdAt` — plus a repository with one derived query method: `findByTenantIdOrderByCreatedAtDesc(UUID tenantId)`.

In `AuthController.login`, record an event on both outcomes:

```java
if (!valid) {
    auditEventRepository.save(new AuditEvent(
            userOpt.map(User::getTenant).orElse(null),
            userOpt.orElse(null),
            "LOGIN_FAILURE",
            "email attempted: " + request.email()));
    return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
}
// ...
auditEventRepository.save(new AuditEvent(user.getTenant(), user, "LOGIN_SUCCESS", null));
```

A failed login with an email that matches no user at all still gets logged — with `null` tenant and actor — because *someone attempting to log in as a nonexistent user* is itself worth a record.

## Step 13 — Add two endpoints that prove isolation, not just assert it

Add `UserRepository.findByTenantId(UUID tenantId)`, then `GET /api/users`:

```java
@GetMapping
public List<Map<String, Object>> listUsersInCurrentTenant() {
    return userRepository.findByTenantId(TenantContext.get()).stream()
            .map(this::toSummary)
            .toList();
}
```

And `GET /api/audit`, same pattern against `AuditEventRepository`. Neither endpoint takes a tenant ID as a parameter anywhere — that's the point. There is no way to ask this endpoint for someone else's tenant's data, because the only tenant it knows how to ask for is whichever one the caller's own verified token says they belong to.

## Step 14 — Prove it, don't just trust it

```bash
# Log in as both tenants' admins, save each token
DEMO_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.serviceflow.os","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

ACME_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@acme.serviceflow.os","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8081/api/users -H "Authorization: Bearer $DEMO_TOKEN"
curl -s http://localhost:8081/api/users -H "Authorization: Bearer $ACME_TOKEN"
```

Each call should return exactly one user — a different one each time. If both calls ever return the same data, or both tenants' users, tenant isolation is broken and nothing past this point in the project is safe to build on.

## Step 15 — Update the domain model doc

Go back to `domain-model.md` and record what actually got built, not just what was planned: mark the "one role per user" open question resolved, note that `Role` really is an enum now (not a table, as first sketched), and add a short "Checkpoint 2 implementation notes" section describing the `TenantContext` pattern — future checkpoints (Customer CRM next) need to follow this same pattern, so it needs to be written down somewhere more permanent than this walkthrough.

## Step 16 — Commit

```bash
git add -A
git commit -m "Checkpoint 2, Day 4: Flyway migrations, tenant isolation, audit trail"
git push
```

## Step 17 — Self-check before calling Day 4 done

- [ ] `flyway_schema_history` shows all three migrations with `success = t`.
- [ ] Two tenants exist, each seeded with one admin user.
- [ ] `GET /api/users` returns *different, tenant-specific* results depending on whose token is used — tested with both tokens, not assumed.
- [ ] `GET /api/audit` shows both a `LOGIN_SUCCESS` and a `LOGIN_FAILURE` event after you deliberately trigger both.
- [ ] `TenantContext.clear()` is called in a `finally` block — check this specifically, it's easy to add the `set()` call and forget the guaranteed cleanup.
- [ ] You can explain why the tenant ID comes from the verified JWT claim and never from a client-supplied parameter.
- [ ] The old `DevDataSeeder` is deleted, not just unused.

If all seven are true, Day 4 is done — Checkpoint 2 (auth) is fully complete. Checkpoint 3 is Customer and location CRM: search, create/edit, multiple contacts and service addresses per customer, all built on top of the tenant-isolation pattern from today.

---

## Appendix — File map for Day 4

```
backend-spring/src/main/resources/db/migration/
├── V1__init_schema.sql       (tenants, users)
├── V2__audit_events.sql
└── V3__seed_demo_data.sql    (two tenants, two admins)

backend-spring/src/main/java/com/serviceflow/serviceflowos/
├── security/TenantContext.java
├── domain/AuditEvent.java
├── domain/AuditEventRepository.java
├── users/UsersController.java     (GET /api/users)
└── audit/AuditController.java     (GET /api/audit)
```

## Appendix — Reference implementation

Compare against the commit titled "Checkpoint 2, Day 4" in this repo's `git log`. The Flyway-autoconfiguration gotcha in Step 2 is specific to Spring Boot 4 at the time this was written — if a future Spring Boot version reunifies autoconfiguration modules again, that step may not reproduce, and that's fine; the diagnostic method (check the dependency tree, then check the actual jar contents) is the durable part, not the specific missing artifact name.
