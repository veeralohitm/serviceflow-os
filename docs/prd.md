# PRD and 8-Week Development Roadmap

## Product name
**ServiceFlow OS** — AI-first field operations platform for home services businesses.

## Overview
ServiceFlow OS is a multi-tenant field service management platform for HVAC, plumbing, electrical, and similar home-services businesses. The platform combines call booking, scheduling, dispatch, jobs, customer and location records, estimates, invoicing, payments, pricebook, memberships, field staff workflows, and AI-assisted automation in a single operating system.[cite:79][cite:99]

Leading home-services platforms group their capabilities around core operations such as call booking, dispatching, jobs, estimates, invoicing, payments, customer records, pricebook, memberships, field mobile tools, and communication.[cite:79] This PRD uses those market-proven product areas as the reference shape, while positioning ServiceFlow OS as a modern AI-first alternative with stronger orchestration and automation.

## Product vision
Build an end-to-end operating system that helps a home-services business move from inbound demand to booked work, completed jobs, invoices, payments, and recurring customer value. The platform should reduce office workload, increase technician utilization, shorten time to cash, and improve customer experience with AI-assisted booking, dispatch, and follow-up.[cite:79][cite:87]

## Primary users
- **CSR / office staff**: handle call intake, create customers, book jobs, reschedule, and collect deposits.
- **Dispatchers**: manage calendars, assign technicians, optimize routes, and monitor live job status.
- **Technicians**: receive jobs, see customer history, complete forms, build estimates, capture signatures, and mark work complete.
- **Managers / owners**: monitor revenue, utilization, memberships, sales conversion, and aging invoices.
- **Customers**: request service, approve estimates, receive notifications, pay invoices, and manage memberships.

## Core use case
A customer calls or submits a web form requesting service. The system identifies or creates the customer and service location, captures the service issue, checks calendars, skills, territory, and arrival windows, then books a job and assigns a technician or queue.[cite:79][cite:100]

As the appointment approaches, the customer receives notifications and the dispatcher can rebalance workloads using a visual board. The technician performs the work, builds an estimate or invoice from the pricebook, captures payment, and triggers follow-up automation such as review requests, unpaid reminders, or maintenance-plan offers.[cite:79][cite:99]

## Goals
- Support the full workflow from lead to cash.
- Provide a scheduler and dispatch board that can handle arrival windows, exact times, buffers, and territory constraints.[cite:100][cite:79]
- Centralize customer, location, job, equipment, and billing data.[cite:79]
- Enable staff and field tech collaboration in real time.
- Layer AI into booking, quoting, dispatch suggestions, and post-job follow-up.
- Be deployable on AWS with CI/CD and production-style version control practices.[cite:101][cite:104]

## Non-goals for MVP
- Full payroll and accounting general ledger.
- Native telephony platform.
- Advanced marketing attribution.
- Complex project management for multi-day construction jobs.
- Marketplace for third-party contractors.

## Product modules

### 1. Calendar and dispatch
The scheduling module will support day, week, and board views; drag-and-drop assignment; blocked time; technician shifts; job duration; buffers; arrival windows; and territory-aware capacity.[cite:79][cite:100] Dispatchers need a live view of unassigned jobs, assigned jobs, in-progress work, completed work, and technician availability.

### 2. Job booking
The booking flow will capture service category, urgency, problem description, preferred time, location, asset/equipment context, and contact preferences. It should support bookings from office staff, online web forms, and future AI-assisted intake channels.[cite:79][cite:99]

### 3. Customer and location management
Maintain customer profiles, contacts, service addresses, communication history, tags, notes, installed equipment, and active memberships. The system should support one customer with multiple service locations and multiple contacts per location.[cite:79]

### 4. Staff and field operations
Maintain employee profiles, roles, skills, certifications, service territories, schedules, PTO, status, and mobile workflow state. Technicians should be able to view assigned jobs, job details, parts/services, notes, forms, and customer history.[cite:79][cite:99]

### 5. Pricing, estimates, billing, and payments
Support a structured pricebook, line items, bundled services, taxes, discounts, deposits, estimates, approvals, invoices, payments, and outstanding balances. ServiceTitan’s market framing shows pricebook, invoicing, and payments as core operational pillars, which makes this module essential to product completeness.[cite:79][cite:87]

### 6. Memberships and recurring service
Support maintenance plans, renewal dates, included services, visit entitlements, and preferred pricing. This module helps model recurring revenue and customer retention patterns commonly used in HVAC and plumbing operations.[cite:79][cite:99]

### 7. Notifications and communications
Support reminder emails, booking confirmations, dispatch notifications, on-the-way notices, estimate approval requests, payment reminders, and review requests. Communication history should be visible on the customer and job records.[cite:79][cite:99]

### 8. AI operations layer
Add AI for call-summary ingestion, booking suggestions, technician-job matching, quote drafting, invoice note generation, upsell prompts, and no-show or churn risk nudges. This becomes the differentiator rather than just cloning a standard FSM product.[cite:79]

## Functional requirements

### Calendar and scheduling
- Create jobs with exact appointment times or arrival windows.
- Apply default service durations by job type.
- Support buffers before and after jobs.[cite:100]
- Prevent double-booking for unavailable technicians.
- Filter availability by territory, skill, and shift.
- Drag jobs between technicians and time slots.
- Show unassigned queue.
- Reschedule jobs and notify customer.

### Booking and job lifecycle
- Create lead, customer, location, and job from a single intake flow.
- Capture job status: new, booked, dispatched, on the way, in progress, completed, invoiced, paid, canceled.
- Attach notes, photos, attachments, and forms.
- Convert estimate to job or invoice where applicable.
- Track emergency priority and SLA.

### Customer management
- Search by name, phone, email, or address.
- View open jobs, invoices, memberships, equipment, and communication history.
- Store consent and contact preferences.
- Track lead source and referral source.

### Staff management
- Create roles for CSR, dispatcher, technician, manager, and admin.
- Store skills, certifications, and service zones.
- Manage work hours, PTO, and active status.
- Track technician utilization and job counts.

### Pricing and billing
- Maintain service and material pricebook entries.
- Build estimates from line items and bundles.
- Convert approved estimates into invoices.
- Accept partial and full payments.
- Support tax rules and discounts.
- Track aging and unpaid balances.

### Notifications
- Send booking confirmations.
- Send reminders before appointment.
- Send technician en route notice.
- Send estimate approval link.
- Send invoice and payment reminder.

### AI features
- Summarize customer intake from notes or transcript.
- Suggest best appointment slot based on rules.
- Recommend technician based on skill and territory.
- Draft estimates and job summaries.
- Generate follow-up suggestions after completion.

## Data model overview
Key entities:
- Tenant
- User
- Role
- Employee
- Skill
- Territory
- Shift
- Customer
- Contact
- Location
- Equipment
- MembershipPlan
- CustomerMembership
- Lead
- Job
- Appointment
- DispatchAssignment
- PricebookItem
- Estimate
- EstimateLine
- Invoice
- InvoiceLine
- Payment
- Note
- Attachment
- Notification
- AuditEvent

## Suggested technical architecture
- **React**: operations console, dispatch board, CSR booking flow, technician mobile-responsive UI.
- **Spring Boot**: core domain services, auth, pricing, job lifecycle, customer, billing, memberships, notifications.
- **Node.js**: real-time event gateway, WebSocket/SSE layer, async communication adapters, AI orchestration helpers.
- **PostgreSQL**: transactional system of record.
- **Redis** (optional): live presence, queues, short-lived dispatch state.
- **AWS**: React on S3/CloudFront or Amplify; Spring Boot and Node on ECS/EC2/Elastic Beanstalk; PostgreSQL on RDS; files on S3.[cite:101][cite:104]
- **CI/CD**: GitHub Actions for lint, test, build, dockerize, and deploy.[cite:101][cite:104]

## User journeys

### Journey 1: Book a job
1. CSR receives request.
2. Search existing customer.
3. Create customer/location if not found.
4. Capture service issue and requested timing.
5. System suggests slots and technician options.
6. CSR books job and triggers confirmation.

### Journey 2: Dispatch and complete
1. Dispatcher views board.
2. Reassigns job to available technician.
3. Technician acknowledges and travels.
4. Technician updates status in field.
5. Technician builds estimate or invoice.
6. Customer signs and pays.

### Journey 3: Post-job retention
1. System sends receipt and review request.
2. AI recommends membership based on job type and asset age.
3. Office follows up on unpaid balances or unsold estimates.

## Epics and tasks

### Epic 1: Platform foundation
- Create repo strategy and branching model.
- Bootstrap React, Spring Boot, and Node services.
- Set up PostgreSQL and migrations.
- Add tenant model and auth base.
- Configure local dev docker-compose.
- Define coding standards and PR template.

### Epic 2: Identity, tenancy, and roles
- Implement sign-in and token auth.
- Add tenant onboarding.
- Add user invitations.
- Implement RBAC for admin, CSR, dispatcher, tech, manager.
- Add audit logging for auth-sensitive actions.

### Epic 3: Customer and location CRM
- Build customer search.
- Build create/edit customer flow.
- Add multiple contacts and service addresses.
- Add notes and communication log.
- Add equipment and membership tabs.

### Epic 4: Staff and operations setup
- Build employee records.
- Add skills, territories, and shifts.
- Add PTO and availability rules.
- Build technician status tracking.

### Epic 5: Job booking engine
- Create intake form and validation.
- Create job types and durations.
- Add appointment generation.
- Build exact-time and arrival-window logic.
- Add booking conflict rules.
- Add booking confirmation workflow.

### Epic 6: Calendar and dispatch board
- Build day/week scheduler UI.
- Build drag-and-drop dispatch board.
- Show unassigned jobs.
- Add filters by territory and skill.
- Add live job status updates.
- Add reschedule and reassignment actions.

### Epic 7: Pricebook, estimates, and invoices
- Create pricebook CRUD.
- Create estimate builder.
- Add estimate approvals.
- Convert estimate to invoice.
- Add taxes, discounts, deposits.
- Build invoice list and detail screens.

### Epic 8: Payments and collections
- Record payments.
- Track unpaid invoices and aging.
- Add payment status updates.
- Add reminder workflows.

### Epic 9: Memberships and recurring service
- Create membership plans.
- Link memberships to customer/location.
- Add recurring visit or renewal logic.
- Apply member pricing and benefits.

### Epic 10: Notifications and communications
- Email templates for reminders and updates.
- Job confirmation and reschedule emails.
- Estimate approval emails.
- Invoice and payment reminders.
- Communication history timeline.

### Epic 11: AI operations layer
- Intake summarization.
- Slot recommendation engine.
- Technician recommendation engine.
- Quote drafting helper.
- Job summary and follow-up suggestion generator.

### Epic 12: Deployment, quality, and release
- Unit and integration tests.
- Seed/demo tenant data.
- CI workflows.
- Container builds.
- AWS staging deploy.
- Smoke tests and production checklist.

## 8-week roadmap
Assumption: 8 hours per day, 5 days per week, 40 hours per week, 320 total hours. Each checkpoint covers 2 days or about 16 hours.

| Week | Checkpoint | Focus | Deliverable |
|---|---|---|---|
| 1 | 1 | Architecture, repo, backlog, environments | Monorepo or multi-repo scaffold, backlog, env strategy |
| 1 | 2 | Auth foundation, tenant model, DB migrations | Login works, tenant-aware tables created |
| 2 | 3 | Customer and location CRM v1 | Search/create/edit customer and location |
| 2 | 4 | Staff, shifts, territories, skills | Employee scheduling foundation complete |
| 3 | 5 | Job intake flow and lifecycle | Job creation end to end |
| 3 | 6 | Scheduling rules and appointment model | Exact time and arrival window support |
| 4 | 7 | Calendar UI and unassigned queue | Scheduler visible and functional |
| 4 | 8 | Dispatch board interactions | Drag/drop assignment and status changes |
| 5 | 9 | Pricebook and estimate builder | Line items and estimate totals working |
| 5 | 10 | Invoice generation and payments v1 | Invoice + manual payment flow |
| 6 | 11 | Notifications and email templates | Confirmation and reminder emails live |
| 6 | 12 | Memberships and recurring service | Plan creation and customer enrollments |
| 7 | 13 | AI intake summary and slot recommendation | AI assistant usable in booking flow |
| 7 | 14 | AI quote draft and follow-up recommendations | AI enhancements on jobs and invoices |
| 8 | 15 | CI/CD, tests, containers | PR checks and deploy pipeline operational |
| 8 | 16 | AWS staging, polish, demo readiness | Staging demo tenant and final hardening |

## Detailed 2-day checkpoint plan

### Checkpoint 1
- Day 1: finalize scope, domain model, repo structure, environment strategy.
- Day 2: bootstrap frontend, backend, realtime service, docker-compose, PostgreSQL.

### Checkpoint 2
- Day 3: auth screens and API security.
- Day 4: tenant isolation, migrations, seed admin user, basic audit trail.

### Checkpoint 3
- Day 5: customer CRUD and search.
- Day 6: location/contact management and timeline shell.

### Checkpoint 4
- Day 7: employee CRUD, role mapping.
- Day 8: shifts, territories, skill matrix, PTO rules.

### Checkpoint 5
- Day 9: create lead-to-job intake flow.
- Day 10: status lifecycle, notes, attachments, validation.

### Checkpoint 6
- Day 11: appointment model and durations.
- Day 12: arrival windows, buffers, conflict detection.

### Checkpoint 7
- Day 13: calendar week/day views.
- Day 14: filters, unassigned queue, calendar persistence.

### Checkpoint 8
- Day 15: drag-and-drop dispatch.
- Day 16: live status updates with WebSocket or SSE.

### Checkpoint 9
- Day 17: pricebook CRUD and bundles.
- Day 18: estimate builder, totals, taxes, discounts.

### Checkpoint 10
- Day 19: invoice creation and estimate conversion.
- Day 20: payment recording, balance tracking, aging buckets.

### Checkpoint 11
- Day 21: email provider integration.
- Day 22: confirmation, reminder, and reschedule templates.

### Checkpoint 12
- Day 23: membership plans and enrollment.
- Day 24: recurring service logic and member pricing rules.

### Checkpoint 13
- Day 25: AI intake summarization.
- Day 26: slot recommendation and technician suggestion.

### Checkpoint 14
- Day 27: quote draft helper.
- Day 28: post-job follow-up suggestions and review prompts.

### Checkpoint 15
- Day 29: test suite, linting, PR checks.
- Day 30: containerization and deploy workflows.

### Checkpoint 16
- Day 31: AWS staging deploy and secrets setup.
- Day 32: bug bash, demo script, release notes, backlog for v2.

## Acceptance criteria for MVP
- Tenant admin can sign in and manage users.
- CSR can create customer, location, and job from one flow.
- Dispatcher can assign and reschedule jobs on a visual board.
- Calendar supports exact times, arrival windows, and conflict handling.[cite:100]
- Technician workflow supports status progression and invoice completion.
- Pricebook, estimate, invoice, and payment flows work end to end.[cite:79][cite:87]
- Notification emails are sent for key events.[cite:79]
- AI can summarize intake and suggest next actions.
- CI/CD deploys the app to staging on AWS.[cite:101][cite:104]

## Suggested delivery sequence
If time gets tight, prioritize in this order:
1. Auth and tenancy.
2. Customer/location CRM.
3. Job intake and lifecycle.
4. Calendar and dispatch board.
5. Pricebook, invoice, payment.
6. Notifications.
7. AI enhancements.
8. Memberships.

## Version control and delivery conventions
- Use trunk-based development with short-lived feature branches.
- Require pull requests for all merges.
- Enforce lint/test/build checks in CI.[cite:101]
- Tag weekly milestones.
- Maintain release notes and deployment checklist.

## Risks and mitigations
- **Scheduling complexity**: start with deterministic rules before optimization.
- **Scope growth**: keep payroll, accounting, and advanced marketing out of MVP.
- **UI complexity**: ship dispatcher board desktop-first, technician workflow mobile-responsive second.
- **AI unpredictability**: keep humans in approval path for quotes and customer-facing messages.
- **Multi-tenancy**: isolate tenant data early in schema and service design.

## Future roadmap after MVP
- Route optimization.
- Native SMS and telephony integrations.
- Customer portal.
- Technician mobile offline mode.
- Advanced reporting and revenue attribution.
- Inventory and purchasing.
- Payroll and commission tracking.
- Deeper AI agent orchestration across booking, dispatch, billing, and retention.[cite:79][cite:99]
