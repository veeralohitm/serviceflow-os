# Day 2 Walkthrough — Checkpoint 1: Bootstrap the Three Services

Hand this doc to someone else along with [`prd.md`](./prd.md) and [`domain-model.md`](./domain-model.md) and they can complete Day 2 entirely on their own — no prior conversation needed. It assumes Day 1 (repo + domain model) is already done.

## What Day 2 actually produces

By the end of today, four things are running on your machine at the same time:
1. **Postgres** — the database, running inside Docker.
2. **backend-spring** — a Spring Boot app that can talk to Postgres.
3. **backend-node** — a Node.js app with a basic web server and a WebSocket channel.
4. **frontend** — a React app you can open in a browser.

None of them do anything useful yet — no login, no customers, no jobs. Day 2 is purely "does the plumbing work," so that every day after this one is about features, not tool setup.

Budget **3–4 hours** if this is your first time touching some of these tools. Most of that time is reading error messages, not writing code — that's normal.

## Before you start

You need, installed and confirmed working:
- **git** — `git --version`
- **Java 21 or newer** — `java -version` (Spring Boot needs this)
- **Node.js 18+ and npm** — `node -v` and `npm -v` (both React and the Node service need this)
- **Docker Desktop** — installed *and running*. Installing the app is not enough; it has to actually be open, because it runs a background service (a "daemon") that the `docker` command talks to.

### Checking Docker specifically

This trips people up, so check it in two steps, not one:

```bash
docker --version          # is the CLI installed?
docker compose version    # is the daemon actually running and reachable?
```

If the first command works but the second fails with something like `unknown command: docker compose`, the CLI is installed but Docker Desktop itself isn't running. Open the Docker.app from Applications (or run `open -a Docker` from the terminal), wait 30–60 seconds for it to fully start, then try `docker compose version` again.

## Step 1 — Understand why there are three separate services first

Before scaffolding anything, be able to answer: *why isn't this just one app?* From the domain model and the PRD's architecture section:

- **frontend** is what a person looks at and clicks — it has no business logic of its own, it just calls the other two.
- **backend-spring** owns the data and the rules — customers, jobs, invoices, and whether a booking is even allowed. This is the one that talks to Postgres.
- **backend-node** does exactly two things and nothing else: pushes live updates to the dispatch board (a job got reassigned — tell the browser *now*, don't make it refresh) and, later, calls out to an AI provider. It's kept separate because those two jobs (long-lived open connections, bursty external API calls) fit Node's style better than Spring's.

If that distinction doesn't make sense yet, re-read the "Suggested technical architecture" section of `prd.md` before continuing — the rest of today only makes sense in light of it.

## Step 2 — Scaffold the React frontend

```bash
rm -f frontend/.gitkeep
npm create vite@latest frontend -- --template react
cd frontend && npm install
```

**What just happened:** `npm create vite@latest` downloads a tool called Vite and uses it to generate a working, minimal React project — folder structure, a dev server, a build step, all pre-wired. You didn't write any of that plumbing by hand; almost nobody does. `npm install` then downloads the actual code libraries (React itself, etc.) that the generated project depends on, into a `node_modules/` folder (already excluded from git — check the root `.gitignore`).

Confirm it works before moving on:

```bash
npm run dev
```

Open the URL it prints (usually `http://localhost:5173`) in a browser. You should see a default Vite+React starter page. Stop it with `Ctrl+C` once confirmed, or leave it running in its own terminal tab.

## Step 3 — Scaffold the Spring Boot backend

Spring has its own generator, [Spring Initializr](https://start.spring.io), which you can drive from a website form or, as here, from the command line:

```bash
rm -f backend-spring/.gitkeep
curl -s "https://start.spring.io/starter.zip" \
  -d dependencies=web,data-jpa,postgresql,devtools,validation \
  -d type=maven-project \
  -d language=java \
  -d javaVersion=21 \
  -d groupId=com.serviceflow \
  -d artifactId=serviceflow-os \
  -d name=ServiceflowOsApplication \
  -d packageName=com.serviceflow.serviceflowos \
  -o /tmp/backend-spring.zip
unzip -o /tmp/backend-spring.zip -d backend-spring
rm /tmp/backend-spring.zip
```

**What each dependency is for** (the `-d dependencies=...` line is the one worth understanding, not memorizing):
- `web` — lets this app respond to HTTP requests (i.e., be a REST API).
- `data-jpa` — a library for reading/writing database rows as Java objects, instead of hand-writing SQL for every query.
- `postgresql` — the specific driver that lets Java talk to a Postgres database.
- `devtools` — auto-restarts the app when you save a code change, so you're not manually stopping/starting it all day.
- `validation` — lets you declare rules like "this field can't be blank" directly on your data objects.

This downloads a `.zip`, so unlike the Vite step, you have to unzip it yourself into the right folder.

**Add one endpoint to prove it works.** Create `backend-spring/src/main/java/com/serviceflow/serviceflowos/HealthController.java`:

```java
package com.serviceflow.serviceflowos;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "backend-spring");
    }
}
```

`@RestController` and `@GetMapping` are *annotations* — Spring's way of saying "this class handles web requests" and "this method handles GET requests to this specific URL." You'll see this pattern constantly from here on.

## Step 4 — Scaffold the Node.js realtime service

No generator here — Node projects usually start smaller and by hand:

```bash
rm -f backend-node/.gitkeep
cd backend-node
npm init -y
npm pkg set type="module"
npm install express ws
```

`npm init -y` creates a bare `package.json` (the project's manifest — name, dependencies, scripts). `npm install express ws` adds two libraries: Express (a minimal web server framework) and `ws` (WebSocket support — the mechanism for a server to push a message to a browser without the browser asking first).

Create `backend-node/index.js`:

```js
import express from "express";
import { createServer } from "http";
import { WebSocketServer } from "ws";

const app = express();
const server = createServer(app);
const wss = new WebSocketServer({ server, path: "/ws" });

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "backend-node" });
});

wss.on("connection", (socket) => {
  socket.send(JSON.stringify({ type: "connected", message: "backend-node realtime channel" }));
  socket.on("message", (data) => {
    socket.send(data); // placeholder relay — Checkpoint 8 wires this to real dispatch events
  });
});

const PORT = process.env.PORT || 4000;
server.listen(PORT, () => {
  console.log(`backend-node listening on http://localhost:${PORT} (WebSocket at /ws)`);
});
```

Add a start script so you don't have to remember `node index.js`. In `backend-node/package.json`, inside `"scripts"`, add:

```json
"start": "node index.js"
```

This service doesn't do anything real yet — it just proves a browser can open a live connection to it. The actual dispatch-board wiring is Checkpoint 8, weeks from now.

## Step 5 — Write docker-compose for Postgres

Create `docker-compose.yml` in the repo root (not inside any service folder):

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: serviceflow
      POSTGRES_USER: serviceflow
      POSTGRES_PASSWORD: serviceflow_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

**Why only Postgres, and not backend-spring or backend-node, in this file?** Two different things are being containerized for two different reasons. Postgres goes in Docker because installing a full database engine directly on your laptop is heavier and messier than pulling a ready-made image. Your own apps stay running natively (`./mvnw spring-boot:run`, `npm start`) for now because that gives you faster restarts, real debuggers, and readable stack traces while you're actively writing code in them. Packaging *your own* services into containers is a deliberate later step — Checkpoint 15 — done once the app is stable enough to be worth freezing into an image.

Bring it up:

```bash
docker compose up -d postgres
docker compose ps
```

`-d` means "detached" — it runs in the background instead of tying up your terminal. `docker compose ps` should show one container, `postgres`, with status `Up`.

## Step 6 — Point Spring Boot at Postgres

Edit `backend-spring/src/main/resources/application.properties`:

```properties
spring.application.name=ServiceflowOsApplication
spring.datasource.url=jdbc:postgresql://localhost:5432/serviceflow
spring.datasource.username=serviceflow
spring.datasource.password=serviceflow_dev

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

The username, password, and database name here have to match `docker-compose.yml` exactly — this file is how Spring Boot finds and logs into the container you just started. `ddl-auto=update` is a dev-only convenience: Hibernate (the library behind `data-jpa`) will auto-create database tables from your Java code. It's fine with zero entities today; real projects replace this with versioned migrations (Flyway or Liquibase) once the schema needs to survive across a team and environments — that switch happens at Checkpoint 2.

## Step 7 — Run everything and verify

Four terminal tabs (or one terminal, backgrounding each with `&`):

```bash
docker compose up -d postgres
cd backend-spring && ./mvnw spring-boot:run
cd backend-node && npm start
cd frontend && npm run dev
```

Then check each one actually responds:

```bash
curl http://localhost:8080/api/health   # -> {"status":"ok","service":"backend-spring"}
curl http://localhost:4000/health       # -> {"status":"ok","service":"backend-node"}
```

...and open the frontend's printed URL in a browser.

### If a port is already taken

You'll sometimes see `Port 8080 was already in use` or Vite silently jumping to 5174, 5175, etc. Before changing anything, find out *what* is using it — don't just kill it blindly, it might be someone else's important, unrelated process:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

This prints the PID and command using that port. If it's clearly something else's long-running project, leave it alone and reconfigure *your* app's port instead — for Spring Boot, add `server.port=8081` to `application.properties`; Vite will pick its own fallback port automatically and print whichever one it chose.

## Step 8 — Commit

```bash
git add -A
git status --short   # sanity check: no node_modules/, no target/, before committing
git commit -m "Checkpoint 1, Day 2: bootstrap all three services + Postgres"
git push
```

The `git status --short` check matters: if `node_modules/` or `target/` show up as new files, your `.gitignore` isn't catching them — stop and fix that before committing, not after (these folders are huge and don't belong in git history).

## Step 9 — Self-check before calling Day 2 done

- [ ] `docker compose ps` shows Postgres `Up`.
- [ ] `curl http://localhost:8080/api/health` (or whatever port you landed on) returns JSON with `"status":"ok"`.
- [ ] `curl http://localhost:4000/health` returns JSON with `"status":"ok"`.
- [ ] The frontend loads in a browser and shows the default Vite+React page.
- [ ] You can explain, in your own words, why Postgres runs in Docker but backend-spring and backend-node don't yet.
- [ ] The commit is pushed and visible on GitHub.

If all six are true, Day 2 is done — Day 3 starts real feature work: login screens and Spring Security.

---

## Appendix — What the generated files actually are

You'll see a lot of new files you didn't write. You don't need to understand every line yet, but know what each *kind* of file is for:

| File | Purpose |
|---|---|
| `frontend/vite.config.js` | Configuration for the dev server and build tool |
| `frontend/src/main.jsx` | The actual entry point — where React "mounts" onto the page |
| `frontend/package.json` | Frontend's dependency list and scripts (`npm run dev`, etc.) |
| `backend-spring/pom.xml` | Maven's dependency list — the Java equivalent of `package.json` |
| `backend-spring/mvnw` | "Maven wrapper" — lets you run Maven commands without installing Maven separately |
| `backend-spring/src/main/java/.../ServiceflowOsApplication.java` | The one file with `public static void main` — where the Spring Boot app actually starts |
| `backend-node/package.json` | Node's dependency list and scripts |

## Appendix — Reference implementation

Compare your result against the real scaffold committed to this repo at Checkpoint 1, Day 2 (`git log` for the commit titled the same way). Small differences — a different Spring Boot patch version, a different fallback port — are expected and fine; the shape (three services, one shared Postgres, one health endpoint each) is what matters.
