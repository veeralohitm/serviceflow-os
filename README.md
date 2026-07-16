# ServiceFlow OS

AI-first field operations platform for home-services businesses (HVAC, plumbing, electrical). Multi-tenant call booking, scheduling, dispatch, jobs, pricing, invoicing, payments, memberships, and AI-assisted automation.

Full requirements: [`docs/prd.md`](docs/prd.md)
Domain model: [`docs/domain-model.md`](docs/domain-model.md)

## Repo layout

```
frontend/         React (Vite) — booking UI, dispatch board, customer records
backend-spring/   Spring Boot (Java) — auth, jobs, billing, customers, memberships
backend-node/     Node.js — WebSocket/SSE realtime, AI orchestration
docs/             PRD, domain model, architecture decisions
docker-compose.yml   local Postgres + services (added Checkpoint 1, Day 2)
```

## Status

Scaffolding in progress — see the epics checklist below. Checked items are done.

## Epics

- [x] Epic 1 — Platform foundation *(repo, domain model, monorepo layout, all 3 services + Postgres running locally)*
- [ ] Epic 2 — Identity, tenancy, and roles
- [ ] Epic 3 — Customer and location CRM
- [ ] Epic 4 — Staff and operations setup
- [ ] Epic 5 — Job booking engine
- [ ] Epic 6 — Calendar and dispatch board
- [ ] Epic 7 — Pricebook, estimates, and invoices
- [ ] Epic 8 — Payments and collections
- [ ] Epic 9 — Memberships and recurring service
- [ ] Epic 10 — Notifications and communications
- [ ] Epic 11 — AI operations layer
- [ ] Epic 12 — Deployment, quality, and release

## Getting started on a fresh clone

### Prerequisites (install once per machine)

- Git
- Java 21+ (JDK)
- Node.js 18+ and npm
- [Docker Desktop](https://docker.com) — installed **and running** (it has to actually be open; installing the app alone isn't enough)

### 1. Clone and install dependencies

```bash
git clone https://github.com/veeralohitm/serviceflow-os.git
cd serviceflow-os

cd frontend && npm install && cd ..
cd backend-node && npm install && cd ..
```

`backend-spring` needs no separate install step — Maven resolves its dependencies automatically the first time it runs.

### 2. Start everything, in order

Four terminal tabs (or background each command):

```bash
docker compose up -d postgres                 # Postgres on localhost:5432

cd backend-spring && ./mvnw spring-boot:run   # http://localhost:8081 — first run also builds the
                                               # schema and seeds demo data automatically (Flyway)
cd backend-node && npm start                  # http://localhost:4000
cd frontend && npm run dev                    # http://localhost:5173 (or next free port)
```

Postgres has to be up *before* `backend-spring` starts, since Flyway needs a database to migrate. The other two can start in any order.

### 3. Try it

Open whatever URL `npm run dev` printed (usually `http://localhost:5173`) and sign in as one of the two seeded demo users:

| Tenant | Email | Password |
|---|---|---|
| Demo HVAC Co | `admin@demo.serviceflow.os` | `admin123` |
| Acme Plumbing | `admin@acme.serviceflow.os` | `admin123` |

**Ports:** Spring Boot defaults to 8080 (this repo pins it to 8081 in `application.properties` since 8080 was already taken on the original dev machine) and Vite defaults to 5173, printing whichever port it actually lands on if that's taken too.

**Database credentials** (local dev only — see `docker-compose.yml`): db `serviceflow`, user `serviceflow`, password `serviceflow_dev`.

**Stopping everything:** `Ctrl+C` each running process, then `docker compose stop` (keeps the data) or `docker compose down` (also fine — the named volume survives either way; only `docker compose down -v` deletes the data).
