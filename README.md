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

## Local development

Start Postgres first, then each service in its own terminal tab.

```bash
docker compose up -d postgres        # Postgres on localhost:5432

cd backend-spring && ./mvnw spring-boot:run   # http://localhost:8081/api/health
cd backend-node && npm start                  # http://localhost:4000/health
cd frontend && npm run dev                    # http://localhost:5173 (or next free port)
```

**Ports:** Spring Boot defaults to 8080 and Vite to 5173, but both are configurable — if something else on your machine already holds those ports, Spring Boot needs `server.port` set in `backend-spring/src/main/resources/application.properties`, and Vite will just print whichever port it fell back to.

**Database credentials** (local dev only — see `docker-compose.yml`): db `serviceflow`, user `serviceflow`, password `serviceflow_dev`.
