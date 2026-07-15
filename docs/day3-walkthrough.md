# Day 3 Walkthrough — Checkpoint 2, Day 3: Auth Foundation

Hand this doc to someone else along with `prd.md`, `domain-model.md`, and the Day 1/Day 2 walkthroughs, and they can complete Day 3 entirely on their own. It assumes Day 2 (all three services running locally against Postgres) is already done.

## What Day 3 actually produces

By the end of today:
1. A person can **log in** with an email and password and get back a signed token.
2. That token **proves who they are** on every request after that — no server-side session, nothing to look up.
3. One backend endpoint is **provably protected**: it rejects requests with no token or a bad one, and works with a good one.
4. A **real login screen** in React drives all of this, styled, not just curl commands.

Budget **4–5 hours**. Security code has more moving parts than CRUD code — expect more re-reading of your own code than usual.

## Before you start

- Day 1 and Day 2 done: repo, domain model, and all three services (`frontend`, `backend-spring`, `backend-node`) running locally against Postgres.
- Comfortable with what a REST endpoint is (Day 2's `/api/health` is one).

## Step 1 — Learn the concept before writing code

**Session auth vs. token (JWT) auth**, in short: with session auth, the server keeps a record of "who's logged in" in its own memory or database, and the browser just holds a session ID cookie that points at that record. With JWT auth, the server keeps *no* record — it hands the browser a signed, self-contained token that already says who the user is, and the browser sends that same token back on every request. The server just checks the signature; it doesn't need to remember anything.

A JWT has three parts, separated by dots: `header.payload.signature`. The payload holds **claims** — plain facts like "subject: admin@demo.serviceflow.os," "role: ADMIN," "expires: 1784166742." The signature is what makes it trustworthy: it's produced by a secret key only the server knows, so if anyone tampers with the payload, the signature won't match anymore and the server rejects it. Paste any JWT into [jwt.io](https://jwt.io) to see this decoded — it's worth doing once so "claims" stops being an abstract word.

**Why this project uses JWT specifically:** the PRD's architecture keeps `backend-spring` stateless on purpose, so it can eventually run as multiple identical instances behind a load balancer with no shared session store to coordinate.

## Step 2 — Add the dependencies

Edit `backend-spring/pom.xml`, adding these alongside the Day 2 dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

`spring-boot-starter-security` is Spring's own auth/authorization framework — it doesn't know about JWT specifically, it just gives you the pluggable filter chain we wire our own JWT logic into. `jjwt` is the library that actually builds and verifies JWTs; it's split into three artifacts (api/impl/jackson) by convention — you only ever write code against `jjwt-api`, the other two are runtime implementation detail.

## Step 3 — Model Tenant, Role, and User

Per `domain-model.md`, `User` belongs to a `Tenant` and has a `Role`. One deliberate simplification worth calling out: **`Role` is implemented as a plain Java `enum`** (`ADMIN`, `CSR`, `DISPATCHER`, `TECHNICIAN`, `MANAGER`), not a database table, even though the domain model doc originally sketched it as reference data. Five fixed, code-defined values don't need a table with a foreign key to get real permission logic later — that's a schema you'd add if roles ever became user-editable, which the PRD doesn't ask for.

Create `backend-spring/src/main/java/com/serviceflow/serviceflowos/domain/Role.java`:

```java
package com.serviceflow.serviceflowos.domain;

public enum Role {
    ADMIN, CSR, DISPATCHER, TECHNICIAN, MANAGER
}
```

Create `Tenant.java` and `User.java` as JPA entities in the same package — `Tenant` needs `id`, `name`, `status`, `createdAt`; `User` needs `id`, a `@ManyToOne` to `Tenant`, `email` (unique), `passwordHash`, `role` (`@Enumerated(EnumType.STRING)`), `status`, `createdAt`. Then a `UserRepository` and `TenantRepository` — plain `JpaRepository<User, UUID>` interfaces, plus one custom method: `Optional<User> findByEmail(String email)`. Spring Data JPA generates the query from that method name alone; you don't write SQL for it.

**Never store a raw password.** `passwordHash` holds the output of a one-way hash (BCrypt, added in Step 4) — the actual password is never saved anywhere, not even encrypted-and-reversible. This is non-negotiable, not a style preference.

## Step 4 — Build the JWT service

Create `backend-spring/src/main/java/com/serviceflow/serviceflowos/security/JwtService.java`. It needs to do exactly two things: turn a `User` into a signed token string, and turn a token string back into verified claims.

```java
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("tenantId", user.getTenant().getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

The signing key needs a real secret — add to `application.properties`:

```properties
jwt.secret=<a long random string, at least 32 bytes>
jwt.expiration-ms=86400000
```

Generate one with `openssl rand -base64 48`. This value is **dev-only** — never commit a production secret to git; real environments load it from an environment variable or a secrets manager.

## Step 5 — Build the filter that checks incoming tokens

This is the piece that runs on *every* request, not just login. Create `security/JwtAuthFilter.java` extending Spring's `OncePerRequestFilter`:

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    // constructor omitted

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                Claims claims = jwtService.extractClaims(token);
                userRepository.findByEmail(claims.getSubject()).ifPresent(user -> {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

**What this actually does, in plain terms:** look for `Authorization: Bearer <token>` on the request. If it's a valid token, look up the user it names and tell Spring Security "this request is authenticated, as this person, with this role." If there's no token, or an invalid one, do nothing and let the request continue anyway — it's the *next* piece (Step 6) that decides whether an unauthenticated request is even allowed to reach its destination.

**A real import gotcha:** `UsernamePasswordAuthenticationToken` lives in `org.springframework.security.authentication`, but the filter class it gets positioned before, `UsernamePasswordAuthenticationFilter`, lives in a *different* package: `org.springframework.security.web.authentication`. Mixing these up produces a confusing "cannot be resolved" error that looks like a missing dependency but is just the wrong import path.

## Step 6 — Wire the security config

Create `security/SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    // constructor omitted

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    // corsConfigurationSource() below
}
```

Three concepts doing real work here:
- **`sessionCreationPolicy(STATELESS)`** tells Spring Security not to create the server-side session it normally would — consistent with "no server-side memory of who's logged in" from Step 1.
- **`permitAll()` vs `authenticated()`** is an allowlist: `/api/auth/login` has to be reachable by someone who isn't authenticated yet (that's the whole point of a login endpoint), everything else defaults to requiring a valid token.
- **`addFilterBefore(jwtAuthFilter, ...)`** is what actually plugs your Step 5 filter into Spring's request pipeline, positioned to run before Spring's own username/password filter.

**CORS**, separately: a browser enforces that JavaScript running on `http://localhost:5173` (React) can't call `http://localhost:8081` (Spring Boot) unless the *server* explicitly says that origin is allowed. This has nothing to do with your JWT logic — it's a browser security rule that exists independent of any auth scheme, and it only applies to browsers, not tools like `curl` (which is why testing with curl alone can hide a CORS bug). Add:

```java
private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

`http://localhost:*` (any port) is used deliberately, since Vite's dev server picks a different port whenever the default is already taken (you hit this in Day 2).

## Step 7 — Build the login and "who am I" endpoints

Two small DTOs (`LoginRequest` with `email`/`password`, `LoginResponse` with `token`/`userId`/`email`/`role`/`tenantId`), then `auth/AuthController.java`:

```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    var userOpt = userRepository.findByEmail(request.email());
    boolean valid = userOpt.isPresent()
            && passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash());
    if (!valid) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }
    User user = userOpt.get();
    String token = jwtService.generateToken(user);
    return ResponseEntity.ok(new LoginResponse(token, user.getId().toString(),
            user.getEmail(), user.getRole().name(), user.getTenant().getId().toString()));
}

@GetMapping("/me")
public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail(),
            "role", user.getRole(), "tenantId", user.getTenant().getId()));
}
```

`passwordEncoder.matches(raw, hash)` re-hashes the submitted password with the same algorithm and compares — you never decrypt a password hash, because BCrypt is one-way by design. `@AuthenticationPrincipal User user` in `/me` works because Step 5's filter set the `User` entity itself as the authenticated principal — Spring hands it straight to the controller, no extra lookup needed.

## Step 8 — Seed one real user to log in as

You can't log in against an empty `users` table. For now, a `CommandLineRunner` bean creates one tenant and one admin user on startup if none exists:

```java
@Component
public class DevDataSeeder implements CommandLineRunner {
    // constructor with TenantRepository, UserRepository, PasswordEncoder omitted

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@demo.serviceflow.os").isPresent()) return;

        Tenant tenant = tenantRepository.findByName("Demo HVAC Co")
                .orElseGet(() -> tenantRepository.save(new Tenant() {{ setName("Demo HVAC Co"); }}));

        User admin = new User();
        admin.setTenant(tenant);
        admin.setEmail("admin@demo.serviceflow.os");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
```

Mark this clearly as **temporary** in a comment. Checkpoint 2, Day 4 replaces it with a versioned Flyway migration — hardcoding seed data in application startup code doesn't survive multiple environments or a team, a migration file does.

## Step 9 — Prove it works with curl before touching the browser

Testing the API directly, with no UI in the way, isolates backend bugs from frontend bugs:

```bash
# wrong password -> 401
curl -s -w "\n%{http_code}\n" -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.serviceflow.os","password":"wrong"}'

# correct password -> 200 + token
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.serviceflow.os","password":"admin123"}'

# /me with no token -> 403
curl -s -w "\n%{http_code}\n" http://localhost:8081/api/auth/me

# /me with the token from above -> 200
curl -s http://localhost:8081/api/auth/me -H "Authorization: Bearer <paste token>"
```

**401 vs. 403, and why both appear:** 401 means "I don't know who you are" (bad credentials at login). 403 means "I know this request has no valid authentication, and this endpoint requires it" (Spring Security's default rejection for a protected route with no token). They look similar but come from different layers — the controller returns 401 deliberately in your own code, while 403 is Spring Security's filter chain rejecting the request before your controller code even runs.

## Step 10 — Build the React login screen

`frontend/src/api.js` — two functions, `login(email, password)` (POSTs to `/api/auth/login`) and `getMe(token)` (GETs `/api/auth/me` with the `Authorization` header). Both just wrap `fetch`.

`frontend/src/Login.jsx` — a form with controlled `email`/`password` state, calling `login()` on submit, and an `onLogin(token)` callback prop so the parent decides what happens with the token rather than `Login` managing app-wide state itself.

`frontend/src/App.jsx` — owns the actual auth state: reads a token from `localStorage` on load (so a page refresh doesn't log you out), fetches `/me` whenever the token changes to get the current user, and renders `<Login>` if there's no valid session or the dashboard view if there is.

```jsx
const [token, setToken] = useState(() => localStorage.getItem("serviceflow_token"));
const [user, setUser] = useState(null);

useEffect(() => {
  if (!token) { setUser(null); return; }
  getMe(token).then(setUser).catch(() => handleLogout());
}, [token]);
```

Storing the token in `localStorage` is a pragmatic MVP choice, not a permanent security decision — it's readable by any JavaScript running on the page, which matters if the app ever loads third-party scripts. Worth a mental "open question" for later, the same way Day 1's domain model tracked open questions rather than pretending everything was settled on day one.

## Step 11 — If the browser can't reach the backend

If curl worked in Step 9 but the browser form fails, open the browser's dev tools console (right-click → Inspect → Console). Two different failure signatures mean two different problems:
- A message mentioning **CORS** or "blocked by CORS policy" → the `corsConfigurationSource()` bean from Step 6 either isn't wired up, or doesn't include the exact origin the browser is running on.
- A generic **network error** with no CORS wording → the backend probably isn't running, or is running on a different port than `api.js` is pointing at (check Day 2's note about ports shifting when defaults are taken).

## Step 12 — Make it look like a real screen, not a debug form

Functionally done doesn't mean done — a login screen a real CSR would use needs actual layout. Build a small design-token system in `index.css` (`--bg`, `--surface`, `--ink`, `--accent`, `--border`, `--radius`, one `--shadow`), support dark mode via `@media (prefers-color-scheme: dark)` redefining the same tokens, then style the card, form fields, error banner, and buttons in `App.css` against those tokens instead of one-off inline styles. Delete any starter-template assets (default Vite/React logos, sample images) that nothing imports anymore — leftover unused files are easy to accumulate and easy to forget to remove.

## Step 13 — Commit

```bash
git add -A
git status --short
git commit -m "Checkpoint 2, Day 3: JWT auth end to end + login UI"
git push
```

## Step 14 — Self-check before calling Day 3 done

- [ ] Wrong password returns 401 with a clear error message.
- [ ] Correct password returns 200 and a token.
- [ ] The protected `/api/auth/me` endpoint returns 403 with no token and 200 with a valid one.
- [ ] The React login screen works end-to-end in an actual browser (not just curl) — sign in, see the logged-in view, log out.
- [ ] You can explain, without looking it up, the difference between 401 and 403 in this system.
- [ ] You can explain why the JWT secret in `application.properties` is dev-only and what would need to change before production.
- [ ] `DevDataSeeder` is clearly marked as temporary, to be replaced by a migration on Day 4.

If all seven are true, Day 3 is done. Day 4 replaces the seed runner with real Flyway migrations and adds tenant isolation and an audit trail.

---

## Appendix — File map for Day 3

```
backend-spring/src/main/java/com/serviceflow/serviceflowos/
├── domain/
│   ├── Role.java              (enum)
│   ├── Tenant.java            (entity)
│   ├── TenantRepository.java
│   ├── User.java              (entity)
│   └── UserRepository.java
├── security/
│   ├── JwtService.java        (sign/verify tokens)
│   ├── JwtAuthFilter.java     (runs per request)
│   └── SecurityConfig.java    (filter chain, CORS, password encoder)
├── auth/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── AuthController.java
└── DevDataSeeder.java          (temporary — replaced Day 4)

frontend/src/
├── api.js
├── Login.jsx
├── App.jsx
├── App.css
└── index.css
```

## Appendix — Reference implementation

Compare against the commit titled "Checkpoint 2, Day 3" in this repo's `git log`. If your login screen's visual styling differs, that's fine — the design tokens and layout are a judgment call, not a correctness requirement. The auth mechanics (JWT structure, 401 vs 403, CORS, stateless sessions) are the part that should match.
