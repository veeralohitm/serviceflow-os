# Day 1 Walkthrough — Checkpoint 1: Architecture, Repo, Backlog

Hand this doc to someone else along with [`prd.md`](./prd.md) and they can complete Day 1 entirely on their own — no prior conversation needed.

## What Day 1 actually produces

By the end of today, three things exist:
1. A **domain model** — the PRD's list of 27 entities turned into real fields and relationships (this is the actual thinking work of the day).
2. A **repo skeleton** — folders that match the project's three-service architecture.
3. A **GitHub repo** with one clean first commit.

Budget **2–3 hours**. The domain model is the hard part; the repo setup is 15 minutes of typing once the model is decided.

## Before you start

You need:
- A computer with `git` installed (check: `git --version`)
- A GitHub account
- 15–20 minutes spent reading `prd.md` first — specifically the **"Data model overview"** and **"Functional requirements"** sections. Don't skip this; Day 1 doesn't work if you haven't read the source material.

Optional but convenient: the [GitHub CLI](https://cli.github.com) (`gh`), installed via `brew install gh` on a Mac. Not required — Step 6 below gives you a non-CLI path too.

---

## Step 1 — Read the entity list, then stop and think

Open `prd.md` and find the **"Data model overview"** section. It lists entity *names only* — Tenant, Customer, Job, Invoice, and so on — with no fields and no relationships. That's the gap Day 1 fills.

Don't start writing yet. First, skim the **"Functional requirements"** section — it's full of clues about what fields each entity needs. For example, this line:

> "Search by name, phone, email, or address."

tells you `Customer` needs, at minimum, a name, phone, and email field, and that `Location` (where "address" lives) is a separate, searchable entity.

## Step 2 — The method: three questions per entity

For every entity in the list, ask:

1. **What fields does it need to do its job?** Hunt for clues in "Functional requirements," not just intuition — the PRD usually says explicitly.
2. **What does it connect to, and how?** One customer can have many locations (one-to-many). One employee can have many skills, and one skill applies to many employees (many-to-many, which usually needs its own join table).
3. **Is it in scope for MVP?** Check the PRD's **"Non-goals for MVP"** section. If a feature is explicitly excluded (e.g. payroll, card processing, a customer portal), don't model fields for it yet.

### Worked example: `Customer`

Applying the three questions:

1. **Fields** — the search requirement above gives us `display_name`, plus the PRD's "Customer management" section adds lead source, referral source, tags, and consent/contact preferences.
2. **Connections** — "one customer with multiple service locations and multiple contacts per location" (stated directly in the PRD) means `Customer 1──* Location` and `Customer 1──* Contact`.
3. **MVP scope** — nothing about Customer is excluded by the non-goals list, so it's fully in scope.

Result:

```
Customer
id, tenant_id, display_name, lead_source, referral_source, tags, created_at

relationships: Customer 1──* Contact
               Customer 1──* Location
               Customer 1──* CustomerMembership
```

Now do the same for the other 26 entities. It's slower than it sounds for the first handful, then speeds up once the pattern clicks.

## Step 3 — Write it down

Create `docs/domain-model.md` in the repo (folder doesn't exist yet — that's fine, `git` and your editor will create it when you save). Use this skeleton, one section per logical group of entities:

```markdown
# ServiceFlow OS — Domain Model

## <Group name, e.g. "Customer & location">

**EntityName**
`field1, field2, field3, ...`

relationships in plain text or arrows
```

Group entities the way the PRD's own "Data model overview" implicitly does: identity/tenancy, staff/operations, customer/location, booking/jobs, pricing/billing, and cross-cutting (notes, attachments, notifications, audit).

End the document with two sections:
- **MVP scope decisions** — a bullet list of anything you simplified or deliberately left out, and why. Future-you (or a teammate) needs this reasoning later; it's easy to forget why a corner was cut.
- **Open questions** — anything you weren't sure about. It's fine to guess and move on — just write the guess down as a question so it gets revisited before Checkpoint 2 (auth), which depends on some of these answers (e.g., can one person hold more than one role?).

## Step 4 — Set up the repo folders

The PRD's architecture section names three services plus a docs folder. Create exactly that shape:

```bash
mkdir -p docs frontend backend-spring backend-node
```

- `frontend/` — the React app (empty until Day 2)
- `backend-spring/` — the Spring Boot app (empty until Day 2)
- `backend-node/` — the Node.js realtime/AI service (empty until Day 2)
- `docs/` — `prd.md` and `domain-model.md` live here

Add a short root `README.md` — project name, one-sentence description, a link to both docs, and the PRD's 12 epics as a markdown checklist (`- [ ] Epic name`). This becomes your running backlog.

## Step 5 — Initialize git

```bash
git init
git branch -m main
```

Add a `.gitignore` before your first commit, so build artifacts and dependency folders never get tracked:

```
.DS_Store
node_modules/
dist/
.env
target/
build/
*.class
.idea/
*.iml
```

Then make the first commit:

```bash
git add -A
git commit -m "Checkpoint 1, Day 1: monorepo skeleton and domain model"
```

**Why a commit today, before any real code exists:** the PRD requires trunk-based development with a full PR history from the start. Starting that habit on Day 1 — not "once there's real code" — is the point.

## Step 6 — Push it to GitHub

**With `gh` CLI installed and authenticated** (`gh auth login` first if needed):

```bash
gh repo create <your-repo-name> --public --source=. --remote=origin --push
```

**Without `gh`** — do it from the browser instead:
1. Go to github.com → New repository → give it a name → don't initialize with a README (you already have one) → Create.
2. GitHub will show you the exact `git remote add origin ...` and `git push` commands for an existing local repo — copy and run those.

## Step 7 — Self-check before calling Day 1 done

- [ ] All 27 entities from the PRD's list appear in `domain-model.md`, each with fields and at least one relationship.
- [ ] Every entity that should be tenant-scoped has a `tenant_id` field (check: does this data belong to one specific home-services business, or is it shared, like `Role`?).
- [ ] The MVP scope section explicitly lists what you deliberately left out and why.
- [ ] At least one open question is written down, even if you already picked a default answer.
- [ ] `git log` shows one commit; `git remote -v` shows a GitHub URL; the repo is visible on github.com.

If all five are true, Day 1 is done — move on to Day 2 (bootstrapping the real React, Spring Boot, and Node apps).

---

## Appendix — Reference answer key

Don't look at this until you've attempted your own version — the thinking is the exercise. Once you have a draft, compare against the version built during this project's own Day 1: [`domain-model.md`](./domain-model.md) in this same `docs/` folder. Differences aren't necessarily wrong — the "Open questions" section exists precisely because reasonable people can model a few of these entities differently (e.g., whether `Job` links to `Location` directly or only through `Customer`).
