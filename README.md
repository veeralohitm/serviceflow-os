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

- [x] Epic 1 — Platform foundation *(in progress: repo, domain model, monorepo layout)*
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

Setup instructions land at Checkpoint 1, Day 2, once `frontend/`, `backend-spring/`, and `backend-node/` are bootstrapped.
