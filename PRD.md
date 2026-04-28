# Product Requirements Document

## Product Name
Rate Limiting Service

## Short Description
A production-grade, distributed API rate-limiting platform built with Spring Boot, Redis, Postgres, and a React observability dashboard. It enforces token-bucket policies, exposes live throttling behavior, and gives operators a way to test, inspect, and tune limits before they impact real customers.

---

## 1. Product Overview

### Vision Statement
Make rate limiting predictable, observable, and easy to operate so teams can protect systems without turning throttling into a hidden source of user pain.

### Mission Statement
Provide a low-latency, distributed rate-limiting control plane that API teams can deploy locally or in production, configure per user or IP, and validate through a dashboard that shows exactly what the system is doing in real time.

### Problem Statement
Most teams either hard-code throttling into services or rely on edge gateways and discover too late that the behavior is opaque. The result is a familiar set of failures:
- Developers cannot reproduce throttling locally.
- Operators cannot see why a request was denied.
- Support teams cannot explain retry timing to customers.
- Security teams cannot consistently enforce abuse controls across services.
- Product teams ship features that work in tests but fail under burst traffic, bot traffic, or auth storms.

This product exists to solve the gap between “we have rate limiting” and “we can prove it works under real traffic, and we can explain it to humans.”

### Target Users

#### 1. Backend Developer
- Needs to protect endpoints from abuse without rewriting business code.
- Wants a simple annotation or middleware layer.
- Cares about minimal latency overhead and deterministic behavior.
- Frustration: hidden rate-limit rules and hard-to-debug 429s.

#### 2. Platform / DevOps Engineer
- Owns rollout, environment config, observability, and reliability.
- Wants Redis/Postgres health, metrics, and fallback behavior visible.
- Cares about deployment repeatability, horizontal scaling, and safe defaults.
- Frustration: rate limiting systems that silently fail open or fail closed without a clear policy.

#### 3. QA / Automation Engineer
- Needs to validate throttling under smoke, burst, and contention scenarios.
- Wants a reliable test harness and repeatable local stack.
- Cares about request logs, 429 thresholds, and retry-after behavior.
- Frustration: tests that pass in isolation but fail at concurrency.

#### 4. Security Engineer
- Uses rate limits to reduce brute force, credential stuffing, scraping, and abuse.
- Wants policy controls by IP, API key, and authenticated user.
- Cares about login throttling, bypass lists, and auditability.
- Frustration: bypass lists that become permanent backdoors.

#### 5. Support / Operations Analyst
- Looks at dashboards when customers report “the API is down.”
- Wants to tell throttling apart from outages and latency spikes.
- Cares about request history, retry-after values, and health status.
- Frustration: no way to prove whether the client or the server caused the failure.

#### 6. Product / Engineering Manager
- Needs confidence that abuse controls do not destroy user experience.
- Wants metrics showing limit utilization, false positives, and adoption.
- Cares about rollout strategy and customer-impact tradeoffs.
- Frustration: no visibility into whether limits are too strict or too lenient.

### User Motivations and Behavioral Psychology
- Users want safety with minimal effort. If the default is hard to configure, they will ship without it.
- Teams tolerate poor observability until the first incident, then they demand explanations immediately.
- Engineers trust systems they can test locally; they mistrust black boxes that only exist in production.
- Operators prefer a slightly conservative default that protects the system over an aggressive default that risks outages.
- End users interpret a 429 differently depending on the explanation. Clear retry messaging reduces frustration and repeated retries.
- Humans overuse bypasses once they exist. Any bypass mechanism must be intentionally narrow, time-bound, and auditable.

### Key Value Proposition
- Distributed token-bucket enforcement with Redis-backed consistency.
- Low-latency request decisions with clear retry-after headers.
- A dashboard that shows throttling behavior instead of hiding it.
- Local and production-friendly deployment using Docker, Spring Boot, React, Prometheus, and Grafana.
- Support for multiple identity dimensions: IP, user, and API key.

### Competitive Advantage
Compared with generic gateways or library-level throttling:
- It is observable by default. The dashboard captures live headers, status codes, and request history.
- It is reproducible locally. Teams can run Redis, Postgres, app, Prometheus, and Grafana together.
- It is code-path close to the product. Rate limit decisions are enforced in the service layer and visible in the UI.
- It is human-readable. Operators can see limit, remaining, reset, and retry-after instead of only “blocked.”
- It supports demo and production auth paths, which makes it useful for both local validation and real resource-server usage.

---

## 2. Market & Reality Analysis

### Current Market Landscape
Rate limiting is usually delivered in one of four ways:
- API gateway or reverse proxy policies.
- Cloud provider edge products.
- Application middleware libraries.
- Redis-backed custom implementations.

Each category solves part of the problem, but most do not combine enforcement, testability, and operational visibility in a single workflow.

### Existing Competitors

#### Direct Competitors
- Kong Gateway rate-limiting plugins.
- NGINX and NGINX Plus request throttling.
- Envoy local/global rate limiting.
- Azure API Management policies.
- AWS API Gateway usage plans and throttling.
- Redis-based custom throttling middleware.

#### Indirect Competitors
- WAF and bot-protection platforms.
- Identity and access management platforms with request controls.
- Observability dashboards that show symptoms but not policy decisions.
- Manual operational playbooks for abuse mitigation.

### Gaps in Current Solutions
- They often sit too far from the application to explain endpoint-specific behavior.
- They are powerful but not easy to test locally under the same configuration as production.
- They expose limits as configuration, not as an operational experience.
- Retry-after behavior is often ignored or inconsistent.
- Many systems optimize for control-plane configuration but not for user-facing diagnostics.

### Why Existing Solutions Fail

#### Technical Reasons
- Network edge systems add hidden latency and dependency complexity.
- Some systems rely on local memory and break down across replicas.
- Redis-less implementations diverge under horizontal scaling.
- Misconfigured clock drift or time windows can make quotas inaccurate.
- High-cardinality metrics can overwhelm observability stacks.

#### UX Reasons
- Operators cannot tell which limit was applied, or why.
- Users receive generic 429 responses with no next-step guidance.
- Rate-limit history is buried in logs instead of surfaced in a dashboard.
- Configuration screens are built for experts, not for fast iteration.

#### Adoption Reasons
- Teams do not trust a system they cannot reproduce in dev.
- “Just add a gateway rule” sounds easy until exceptions, bypasses, and multi-tenant rules arrive.
- Product teams hesitate if the system cannot show a clear rollback path.
- Developers avoid systems that make local setup difficult.

### Real-World Constraints
- Internet connectivity is unreliable in many regions, especially for day-to-day operator workflows in India and emerging markets.
- Many developers use low-end laptops or shared cloud VMs, so the UI and local stack must stay lightweight.
- Browser tabs are often left open for hours; health checks, token expiration, and stale data must handle this gracefully.
- Users will retry aggressively when blocked, so the product must make 429 responses self-explanatory and stable.
- People will misconfigure bypass lists if the UI makes them too easy to add or too hard to review.
- Rate limits must continue to work under Redis pressure, partial outages, and bursty auth traffic.

### Regional Constraints: India + Global Scalability
- India-first usage patterns often involve mobile hotspots, variable latency, and inconsistent office networks.
- A practical product must tolerate slow reloads and recover without losing request history.
- Public cloud costs matter, so default design should minimize Redis chatter and unnecessary polling.
- Time zones and locale formatting should be configurable because teams collaborate across India, Europe, and the US.
- Audit logs and policy history should be exportable for compliance reviews across jurisdictions.

---

## 3. Core Features

### 3.1 Authentication

#### Feature Description
Support authenticated access to the platform and protected APIs using JWT-based login in demo/local mode and external JWT/OIDC validation in production mode.

#### Why It Exists
- Prevents anonymous abuse.
- Enables user-based quotas and audit trails.
- Allows the dashboard to make protected API calls with a stored token.

#### User Flow
1. User opens the dashboard.
2. System checks whether the current environment is demo/local or production.
3. In demo/local mode, user signs in with username and password.
4. Backend validates credentials, issues JWT, and returns token metadata.
5. Dashboard stores the token for subsequent requests.
6. Protected API calls include `Authorization: Bearer <token>`.
7. On expiry, the user is redirected to sign in again.

#### Backend Logic
- Validate password hash against stored user record.
- Issue signed JWT with role and expiry claims.
- Support local JWKS endpoint for verification.
- In resource-server mode, validate against external JWKS and issuer.
- Apply rate limits to login endpoints separately from business APIs.

#### Data Flow
User credentials -> auth controller -> user repository -> password encoder -> JWT issuer -> client token storage -> authenticated API requests.

#### Edge Cases
- Expired JWT.
- Invalid issuer or audience in production.
- Missing Authorization header.
- Login attempts from whitelisted IPs that still need review.
- Multiple tabs with stale tokens.

#### Failure Scenarios
- Redis unavailable and login throttling becomes impossible.
- JWT signing key rotation breaks local verification if JWKS cache is stale.
- Password hash mismatch due to seeded data drift.

#### Abuse / Misuse Scenarios
- Credential stuffing.
- Brute-force login.
- Token replay from a copied browser session.
- Repeated login attempts to exhaust auth capacity.

#### Performance Considerations
- Login should remain sub-200 ms in local and typical production environments.
- JWT verification should be stateless and cache-friendly.
- Login rate limiting must avoid adding excessive DB reads for failed attempts.

---

### 3.2 Dashboard

#### Feature Description
A central operational view showing request counts, success/throttle/error totals, recent response-time shape, and a live request tester.

#### Why It Exists
- Gives non-technical stakeholders a way to understand rate limiting behavior.
- Allows rapid iteration on policy values.
- Serves as the first troubleshooting surface when users report throttling.

#### User Flow
1. User lands on Dashboard.
2. Health status loads and refreshes periodically.
3. User sees aggregate request metrics and a performance trend.
4. User can run ping, login, list products, or create products.
5. Each request updates toast notifications and history.
6. Dashboard retains the latest 40 requests for inspection.

#### Backend Logic
- Ping endpoint reports service health.
- Auth and product endpoints return request headers used to infer rate-limit status.
- Metrics are derived from recent request history and health polling.

#### Data Flow
Client action -> API call -> response timing + headers -> request history entry -> metrics aggregation -> dashboard render.

#### Edge Cases
- First load before any request exists.
- Token present but expired.
- Health check succeeds while downstream Redis fails for protected calls.
- Rapid clicking produces overlapping requests.

#### Failure Scenarios
- API base URL misconfigured.
- Health endpoint unavailable due to server outage.
- Request history grows stale after prolonged idle period.

#### Abuse / Misuse Scenarios
- Users spam the tester to trigger unnecessary load.
- Operators misread synthetic performance charts as real traffic analytics.

#### Performance Considerations
- UI should remain responsive with the last 40 calls only.
- Derived metrics should be computed client-side without blocking renders.
- Health polling should be conservative enough to avoid noise, but frequent enough to detect outages.

---

### 3.3 Core Rate-Limiting Functionality

#### Feature Description
Distributed token-bucket enforcement using Redis and Lua scripts, with configurable limits by IP, user, or API key.

#### Why It Exists
- This is the actual product core: decide whether a request may proceed.
- The goal is deterministic throttling that works across multiple service replicas.

#### User Flow
1. Incoming request hits the application filter or annotation point.
2. System identifies the limiting dimension.
3. Policy resolver loads the matching rule set.
4. Redis Lua script performs atomic token-bucket update.
5. Request is allowed or denied.
6. Headers are attached to response so the client can understand the outcome.
7. Decision is logged for observability.

#### Backend Logic
- Determine policy precedence: endpoint override > user-defined rule > default limit.
- Compute bucket refill based on request timestamp and configured refill rate.
- Decrement token only when capacity exists.
- Return limit, remaining, reset, and retry-after metadata.
- If Redis is unavailable, use fallback policy `DENY` for safety in sensitive paths.

#### Data Flow
Request metadata -> policy resolution -> Redis bucket state -> Lua decision -> response headers -> metrics/logging.

#### Edge Cases
- Clock skew between app nodes.
- Bursty requests at bucket boundary.
- Multiple requests from same user and IP at once.
- Missing API key header when rule expects API key dimension.
- Empty whitelist and false positive bypass assumptions.

#### Failure Scenarios
- Redis timeout.
- Lua script error.
- Circuit breaker opens after repeated Redis failures.
- Misconfigured burst capacity causes too many 429s.

#### Abuse / Misuse Scenarios
- Client retries instantly without respecting retry-after.
- Clients rotate IPs to evade IP-based limits.
- Internal users share credentials and collapse user-based fairness.
- Attackers probe rate-limit thresholds to map service capacity.

#### Performance Considerations
- Rate-limit check should remain near constant time.
- Redis round trips must be minimized.
- Lua script should be cached and reused.
- Metrics must avoid per-request high-cardinality labels.

---

### 3.4 Notifications

#### Feature Description
Transient toast messages for success, warnings, and errors in response to user actions.

#### Why It Exists
- Users need instant feedback when requests fail or throttle.
- It prevents silent confusion when the UI issues a request but nothing appears to happen.

#### User Flow
1. User runs an action.
2. Response returns.
3. UI shows success, warning, or error toast.
4. User can dismiss the message manually.

#### Backend Logic
- Backend returns structured status and rate-limit metadata.
- Frontend interprets 429 as a special warning with retry-after guidance.

#### Edge Cases
- Multiple toasts queued in quick succession.
- Offline state after a previously successful action.
- Dismissed message while another request is in flight.

#### Failure Scenarios
- Error responses without machine-readable body.
- Toast obscures important page content on small screens.

#### Performance Considerations
- Toast rendering should be lightweight and never block the main thread.

---

### 3.5 File Handling

#### Feature Description
Policy import/export, CSV exports, and optional downloadable reports for audit and support workflows.

#### Why It Exists
- Teams need to back up or replicate policies.
- Operators need portable evidence during incident reviews.
- Support often needs a shareable artifact instead of a screenshot.

#### User Flow
1. Admin exports current policies or request history as JSON or CSV.
2. File downloads locally.
3. Admin can later upload a policy bundle to seed a new environment.

#### Backend Logic
- Serialize policy records, whitelists, and thresholds.
- Validate uploaded schema before applying changes.
- Reject unsupported versions with a readable error.

#### Data Flow
Database or config store -> export service -> file download; or upload -> parser -> validator -> policy updater.

#### Edge Cases
- Large export on low-memory browser.
- Schema mismatch across versions.
- Partial import where some records are invalid.

#### Failure Scenarios
- Corrupted file upload.
- Permission denied to access export data.
- Download interrupted mid-transfer.

#### Abuse / Misuse Scenarios
- Malicious policy upload that disables all protection.
- Exports containing sensitive tokens or internal endpoints.

#### Performance Considerations
- Exports should stream when datasets grow.
- Upload parsing should run server-side with size limits.

---

### 3.6 Real-Time Features

#### Feature Description
Near-real-time dashboard updates for health state, latest request history, and latest rate-limit header snapshot.

#### Why It Exists
- Operators need immediate feedback while tuning limits.
- It shortens the loop between an action and its observed effect.

#### User Flow
1. Dashboard loads.
2. Health status polls on a fixed interval.
3. Latest request metadata updates after each action.
4. Rate-limits page shows the latest header snapshot.

#### Backend Logic
- Ping endpoint for health polling.
- Request history maintained on client side for the session.

#### Edge Cases
- Browser tab suspension causing delayed timers.
- Stale data after long inactivity.

#### Failure Scenarios
- Polling keeps failing while other endpoints succeed.

#### Performance Considerations
- Prefer polling for health and request-side refresh for headers before introducing websockets.
- Avoid pushing every internal state change in real time unless operationally necessary.

---

### 3.7 Gamification

#### Feature Description
Not a core product requirement. Optional internal-only progress cues may be used for adoption but must never obscure operational truth.

#### Why It Exists
Only if needed to encourage completion of setup or policy rollout tasks.

#### Decision
Do not ship consumer-style gamification in the MVP. It adds noise and can reduce trust in a control-plane product.

---

### 3.8 Analytics

#### Feature Description
A simple operational analytics view that turns recent request history into a health and throughput interpretation.

#### Why It Exists
- Lets teams understand whether limits are protecting the system or over-throttling it.
- Gives a fast qualitative read before deeper observability analysis.

#### User Flow
1. User opens Analytics.
2. UI plots recent history.
3. User interprets trends in response speed and throttling.
4. User uses the output to adjust policies or investigate bottlenecks.

#### Backend Logic
- Combine status codes and duration values into basic operational indicators.
- No ML required for MVP.

#### Edge Cases
- Too little traffic to make trends meaningful.
- Burst-heavy test traffic producing misleading spikes.

#### Performance Considerations
- Client-side calculations are sufficient for the current dataset size.

---

### 3.9 Admin Panel

#### Feature Description
Restricted operator interface for managing users, roles, policies, whitelists, environment toggles, and audit visibility.

#### Why It Exists
- Production systems need segregation of duties.
- Bypass and limit changes should not live inside ad hoc code changes.

#### User Flow
1. Admin signs in.
2. Admin opens secure settings area.
3. Admin edits policy thresholds and whitelists.
4. System validates and stores the change.
5. Change is audited and propagates to rate-limit evaluation.

#### Backend Logic
- RBAC check on every admin route.
- Version policy objects so changes can be rolled back.
- Log before/after values with actor identity.

#### Edge Cases
- Concurrent edits from two admins.
- Policy deleted while requests are in flight.

#### Failure Scenarios
- Unauthorized role attempting admin action.
- Invalid policy causing all traffic to be blocked.

#### Performance Considerations
- Admin reads are low-volume, but policy reads must be fast because request paths depend on them.

---

## 4. User Experience (UX/UI)

### Screen-by-Screen Breakdown

#### 1. Login Screen
- Username/password fields for demo/local use.
- Clear error copy for invalid credentials and rate-limit lockouts.
- Optional SSO buttons in production.
- No aggressive marketing content; keep it operational.

#### 2. Dashboard Screen
- Top-level request totals.
- Success / throttled / error cards.
- Performance line chart.
- API tester for ping, login, get products, create product.
- Latest response context below the tester.

#### 3. Analytics Screen
- Operational trend chart.
- Short interpretation panel with plain-language guidance.

#### 4. Rate Limits Screen
- Latest X-RateLimit and Retry-After snapshot.
- Request ledger table with last 40 calls.
- Clear-history action.

#### 5. Settings Screen
- Current API base URL display.
- Environment instructions and operator notes.
- Clear indication of local vs configured target.

#### 6. Admin Screen
- Policy list, rule editor, whitelist editor, audit log, export/import controls.
- Strong confirmation steps for destructive changes.

### Navigation Structure
- Left sidebar for primary navigation.
- Top navbar for health state and sidebar collapse.
- Mobile navigation should reduce to stacked sections or a bottom drawer if the screen becomes too narrow.

### Interaction Design
- Primary actions should be visible without scrolling.
- Destructive actions need confirmation and a readable preview of the change.
- Limit-setting fields should explain units directly in labels, not in hidden help text.
- Successful auth should produce immediate visual confirmation and token persistence.

### Mobile-First vs Desktop Strategy
- Mobile must support quick inspection, login, and viewing rate-limit status.
- Desktop remains the primary operator workspace for policy editing and request analysis.
- Charts and tables should collapse gracefully rather than forcing horizontal scrolling.

### Accessibility Considerations
- Keyboard navigation across all controls.
- Sufficient color contrast for success, warning, and error badges.
- Screen-reader labels for request tester controls and live status indicators.
- Avoid status-only color cues; pair with text labels.
- Toasts should use `aria-live` politely and not interrupt the user.

### Micro-Interactions
- Loading skeletons for metric cards.
- Immediate toast on 429 with retry guidance.
- Small animation for sidebar collapse rather than a jarring jump.
- Clear hover states on history rows and action buttons.

### Loading / Empty / Error States
- Loading: skeleton cards and muted controls.
- Empty: explicit “No traffic yet” or “No headers captured yet” messages.
- Error: readable endpoint-level error message with status code.
- Offline: health state must surface even if the dashboard still renders.

---

## 5. Technical Architecture

### System Architecture
- Frontend: React + Vite dashboard.
- Backend: Spring Boot 3 API service.
- Database: Postgres for users, policies, and audit data.
- Cache / coordination: Redis for distributed token-bucket state.
- Observability: Prometheus metrics and Grafana dashboards.
- Local orchestration: Docker Compose.

### Suggested Tech Stack
- Spring Boot 3: mature security, metrics, validation, and actuator support.
- Redis: atomic distributed counters and token-bucket state.
- PostgreSQL: durable storage for users, policies, audit logs, and environment config.
- React 19 + Vite: fast dashboard iteration and lightweight deployment.
- Spring Security + JWT: stateless auth and resource-server compatibility.
- Flyway: versioned schema migration.
- Testcontainers: realistic integration testing.
- Prometheus + Grafana: operational metrics and dashboards.

### Data Models

#### Users
- `id`
- `username`
- `password_hash`
- `role`
- `status`
- `created_at`
- `last_login_at`

#### Policies
- `id`
- `name`
- `scope` (global, endpoint, role, user, IP, API key)
- `dimension`
- `requests_per_minute`
- `burst_capacity`
- `priority`
- `enabled`
- `created_by`
- `updated_by`
- `version`
- `created_at`
- `updated_at`

#### Whitelists / Bypass Rules
- `id`
- `policy_id`
- `type` (IP, API key, user)
- `value`
- `expires_at`
- `reason`
- `created_by`
- `approved_by`

#### Request / Decision Audit
- `id`
- `request_id`
- `principal_id`
- `endpoint`
- `dimension`
- `limit_value`
- `remaining_value`
- `decision` (allow / deny)
- `retry_after_seconds`
- `latency_ms`
- `created_at`

### API Structure

#### Auth
- `POST /api/auth/login`
- `POST /api/auth/logout` later if session invalidation is needed
- `GET /api/auth/me`
- OIDC callback endpoints for production SSO later

#### Product / Demo Business Endpoints
- `GET /api/v1/products`
- `POST /api/v1/products`
- `GET /api/v1/products/admin`

#### Health and Observability
- `GET /ping`
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /.well-known/jwks.json`

#### Admin / Policy APIs
- `GET /api/admin/policies`
- `POST /api/admin/policies`
- `PUT /api/admin/policies/{id}`
- `DELETE /api/admin/policies/{id}`
- `GET /api/admin/audit`
- `POST /api/admin/policies/import`
- `GET /api/admin/policies/export`

### Authentication Flow
- Demo/local: username/password -> JWT issuance -> bearer token on each request.
- Production: OIDC/JWT verification via issuer and JWKS.
- API access is role-aware; admin routes require elevated roles.
- Login endpoint should be rate-limited more aggressively than normal APIs.

### Storage Strategy
- Redis stores ephemeral bucket state and refill timestamps.
- Postgres stores durable entities and audit history.
- No file storage is required for the MVP beyond exports/imports, which can be streamed downloads or cloud object storage later.
- Keep secrets outside the database and inject them through environment variables or secret managers.

### Scalability Approach
- Keep app nodes stateless.
- Share all rate-limit decision state through Redis.
- Scale backend horizontally behind a load balancer.
- Use Postgres connection pooling and read indexes for admin queries.
- Partition or archive audit data once volume grows.

### Performance Optimization Techniques
- Cache policy definitions in memory with short TTL.
- Use atomic Lua scripts to avoid multi-command race conditions.
- Limit UI history size to avoid browser memory growth.
- Avoid overly frequent polling for non-critical data.
- Keep metrics labels low-cardinality.

---

## 6. AI / Advanced Features

### AI Usage
No AI is required for the MVP. Rate limiting is fundamentally deterministic and should remain rule-based.

### Future AI Opportunities
- Recommend default policies based on historical traffic.
- Detect anomalous spikes or bot-like behavior.
- Summarize incidents for operators.

### Model Type
- MVP: rule-based.
- Future: lightweight anomaly detection, then optional LLM-based explanation layer.

### Input / Output Flow
- Input: request history, policy history, and limit utilization.
- Output: suggested thresholds, anomaly flags, and plain-language summaries.

### Latency Considerations
- AI must never sit in the request enforcement path.
- Any analysis must be asynchronous and read-only.

### Cost Considerations
- AI should be opt-in because inference cost can exceed the cost of the rate-limit system itself if run per request.

### Fallback Mechanisms
- If AI fails, the system continues with rule-based policy enforcement.
- No request should be blocked because an AI service is unavailable.

---

## 7. Security & Privacy

### Authentication Security
- Hash passwords using a strong adaptive algorithm.
- Short-lived JWTs with explicit expiration.
- Validate issuer, audience, and signature.
- Support key rotation.

### Data Protection
- Minimize PII; store only what is required for access control and auditing.
- Encrypt secrets in transit and at rest.
- Restrict logs so they never include bearer tokens or raw passwords.

### API Security
- Require bearer tokens for protected routes.
- Enforce RBAC on admin APIs.
- Add request validation on all inputs.
- Return generic auth errors to avoid user enumeration.

### Abuse Prevention
- Rate-limit login, password reset, and token exchange endpoints.
- Add IP or fingerprint-based heuristics for abuse escalation.
- Monitor repeated 429 responses as a signal of misuse or bots.

### Rate Limiting
- Default to fail-closed for sensitive endpoints when the limiter cannot make a safe decision.
- Allow narrow and auditable bypasses only.
- Ensure bypass rules expire and are reviewable.

### GDPR / Indian Compliance Considerations
- Provide a data retention policy for audit logs.
- Support export and deletion workflows for user data where legally required.
- Respect the Indian DPDP Act by minimizing personal data, documenting purpose, and supporting deletion or retention control.
- Make regional hosting decisions explicit for customers with residency requirements.

### Threat Modeling
- Brute force login.
- JWT theft and replay.
- Redis outage leading to open or closed behavior.
- Policy tampering by an over-privileged admin.
- Header spoofing for IP or API key based limits.
- Replay of stale browser actions after token expiration.

---

## 8. Edge Cases & Failure Handling

### Network Failures
- UI should show offline or degraded state without crashing.
- Requests should surface retry guidance when the backend is unreachable.
- Dashboard health polling should recover automatically.

### Partial Data Corruption
- Bad policy records should be rejected at validation time, not at request time.
- Corrupt audit rows should not stop the entire admin console.

### Concurrent Usage Conflicts
- Two admins editing the same policy must trigger version checks or optimistic locking.
- Concurrent requests from the same principal must preserve token-bucket correctness.

### Server Downtime
- Health indicator must switch to offline promptly.
- Fallback policy should be explicit and safe.
- Operators need a clear recovery path after Redis or Postgres restart.

### Unexpected User Behavior
- Rapid repeated clicks.
- Invalid product forms.
- Requests with missing tokens.
- Browser refresh during an in-flight request.

### Recovery Mechanisms
- Circuit breaker around Redis rate-limit checks.
- Retry only for safe read operations.
- Dead-letter or audit queue later for write-heavy admin actions.
- Clear admin rollback for policy changes.

---

## 9. Performance & Scalability

### Expected User Load
- MVP: dozens of internal users and a few thousand API requests per minute across test and demo traffic.
- Production target: scale to hundreds of services and tens of thousands of rate-limit decisions per second if Redis and backend sizing are tuned.

### Bottlenecks
- Redis round trips under high concurrency.
- Policy lookup latency if not cached.
- Database writes for audit trails.
- Frontend metrics calculations if history is allowed to grow unbounded.

### Caching Strategies
- Cache policy configs in memory with short TTL and explicit invalidation.
- Cache JWKS responses with standard TTL.
- Keep UI request history capped.

### Load Balancing
- Stateless application nodes behind a standard HTTP load balancer.
- No session affinity required for core enforcement.

### Database Optimization
- Index by username, role, policy scope, and created_at.
- Archive old audit data.
- Avoid heavy joins on the request path.

### Horizontal Scaling Plan
- Add app replicas first.
- Scale Redis with persistence and high availability.
- Separate read-heavy admin analytics from write-heavy audit ingestion if volume grows.
- Introduce queue-based audit processing only when needed.

---

## 10. Business Model & Monetization

### Revenue Streams
- Hosted SaaS subscription for API teams.
- Enterprise self-hosted license.
- Support and onboarding services.
- Premium observability and policy analytics.

### Pricing Model
- Free developer tier for local and small-scale testing.
- Team tier priced by protected services or monthly active requests.
- Enterprise tier with SSO, audit exports, policy approvals, and high availability.

### Cost Structure
- Redis and Postgres hosting.
- Observability stack costs.
- Support and customer success.
- Engineering maintenance for policy compatibility and auth integrations.

### Profitability Strategy
- Keep the core limiter lightweight so unit economics stay favorable.
- Charge for operational conveniences: multi-tenant admin, analytics, approvals, exports, and compliance.
- Avoid per-request AI or expensive synchronous processing.

### Growth Strategy
- Win by being easy to test locally and easy to explain to non-experts.
- Use integration templates for common stacks.
- Target platform teams that need clear 429 behavior rather than just gateway configuration.
- Expand through developer trust, not marketing abstraction.

---

## 11. Development Roadmap

### Phase 1: MVP
**Timeline:** 3 to 5 weeks

Must-have scope:
- JWT login for demo/local mode.
- Resource-server validation for external JWTs.
- Redis-backed distributed rate limiting.
- Default limits plus endpoint overrides.
- Dashboard with request tester, metrics, and rate-limit snapshots.
- Health and Prometheus metrics.
- Docker Compose local stack.
- Basic audit logging.

### Phase 2: Enhanced Features
**Timeline:** 4 to 6 weeks

Add:
- Admin panel for policy management.
- Role-based access control improvements.
- Policy import/export.
- Better operational analytics and trend comparisons.
- More granular whitelisting and expiration rules.
- Improved error messaging for clients and operators.

### Phase 3: Scaling & Optimization
**Timeline:** 4 to 8 weeks

Add:
- Policy caching and invalidation improvements.
- Multi-node observability and alerting.
- Audit archival jobs.
- Stronger circuit breaker and fallback policies.
- Load-test automation and capacity benchmarks.
- Support for multi-region deployment planning.

### Phase 4: Advanced Features
**Timeline:** ongoing

Add:
- Policy recommendation engine.
- Anomaly detection.
- Approval workflows for sensitive policy changes.
- OpenAPI import and SDK generation.
- Tenant-level analytics and reporting.
- Gateway and edge integrations.

---

## 12. Testing Strategy

### Unit Testing
- Auth success and invalid credential paths.
- JWT issuance and validation behavior.
- Policy resolution precedence.
- Token-bucket math and refill logic.
- UI state rendering for loading, empty, and error states.

### Integration Testing
- Redis-backed rate-limit decisions under real Redis.
- Postgres persistence and migrations.
- Auth/login flow with seeded users.
- Protected API requests with valid and invalid JWTs.
- Dashboard request tester against the actual backend.

### System Testing
- Full local environment using Docker Compose.
- End-to-end browser flow from login to throttling to analytics.
- Admin policy edits followed by enforcement verification.

### Load Testing
- Burst traffic and steady-state traffic.
- Same-principal contention tests.
- 10k req/s target validation in a realistic environment, not on a laptop benchmark.
- Retry-after correctness under saturation.

### Security Testing
- Brute-force login simulation.
- JWT tampering.
- Header spoofing attempts for IP and API key dimensions.
- Unauthorized admin route access.

### Real-World Scenario Testing
- Expired token mid-session.
- Redis restart during live traffic.
- Browser reload during in-flight request.
- Client ignoring retry-after and retrying in a loop.

### Test Case Examples
- Login is rate-limited after repeated bad attempts.
- GET products returns 401 without token and 200 with token.
- POST products is throttled when burst capacity is exceeded.
- Admin product endpoint bypasses limit only for authorized admin users.
- Dashboard shows 429 count after intentional throttling.

---

## 13. Deployment & DevOps

### CI/CD Pipeline
- Build backend with Maven.
- Run unit tests and integration tests.
- Build frontend with Vite.
- Run lint checks.
- Produce Docker image and validate compose stack.
- Deploy through GitHub Actions, GitLab CI/CD, or Azure DevOps depending on customer choice.

### Hosting Strategy
- Backend container on a managed container platform or standard VM/container service.
- Frontend as static assets on a CDN or static hosting layer.
- Redis and Postgres as managed services in production.
- Prometheus/Grafana for observability environments and demos.

### Monitoring Tools
- Spring Boot Actuator.
- Prometheus metrics.
- Grafana dashboards.
- Application logs with correlation IDs.
- Health probes at the container or platform level.

### Logging System
- Structured application logs.
- Separate request decisions from debug noise.
- Log 429s and limiter fallbacks explicitly.
- Never log secrets or full JWTs.

### Backup & Recovery
- Postgres backups on a defined schedule.
- Redis persistence if policy state or long-lived counters require it.
- Documented restore steps for both data stores.
- Ability to re-seed demo data for local environments.

---

## 14. Success Metrics (KPIs)

### User Engagement Metrics
- Weekly active operators using the dashboard.
- Number of request tests run per environment.
- Number of policy changes made through the UI.
- Number of exported reports or audit files.

### Retention Metrics
- 30-day return rate for teams that configure at least one policy.
- Percentage of pilot teams still using the product after first incident review.

### System Performance Metrics
- Rate-limit decision latency p95 and p99.
- Redis error rate.
- 429 correctness rate.
- Health-check uptime.
- Time to recover after Redis or Postgres restart.

### Business KPIs
- Number of protected services per customer.
- Monthly recurring revenue.
- Conversion from local/demo usage to production deployment.
- Support ticket volume per tenant.
- Reduction in abuse-related incidents after adoption.

---

## 15. Risks & Limitations

### Technical Risks
- Redis outages can affect enforcement behavior.
- Misconfigured fallback mode may be too strict or too permissive.
- Clock skew can distort retry-after calculations.
- Policy complexity can create edge-case bugs.

### Business Risks
- Teams may already rely on gateway throttling and resist a separate control plane.
- If observability is weak, users will not trust the system enough to adopt it.
- If setup is too complex, the product will be used only for demos.

### User Adoption Risks
- Operators may avoid policy changes if the approval and rollback process is too slow.
- Developers may bypass the system if local setup is painful.
- Overly aggressive defaults may cause internal teams to disable the product after the first false positive.

### External Dependencies
- Redis availability and performance.
- Postgres reliability.
- Browser and frontend runtime stability.
- External OIDC provider behavior in production mode.
- Platform-specific container limits and networking rules.

---

## 16. Future Expansion

### Features to Add Later
- Multi-tenant organizations and workspaces.
- Policy approval workflows.
- OpenAPI-based policy scaffolding.
- Automated anomaly detection.
- Customer-visible throttling pages and SDK helpers.
- Per-route analytics and abuse forensics.

### Integration Opportunities
- API gateways and service meshes.
- SIEM and alerting tools.
- ChatOps notifications for policy changes.
- Ticketing integrations for approval flows.
- Identity providers like Google, GitHub, Okta, Azure AD, and Supabase.

### Platform Expansion
- Mobile-friendly operator view for quick status checks.
- Public API for policy management.
- CLI tooling for local policy injection and smoke testing.
- SDKs for Java, Node, Python, and Go.
- Cloud-hosted managed service with regional deployment options.

---

## Appendix: Product Scope Decision
This PRD intentionally treats the current repository as an observability-first, distributed rate-limiting platform rather than a generic dashboard app. The current implementation already supports the core enforcement loop, demo auth, live request testing, metrics, and local production-like packaging. The roadmap extends those foundations into a full operator product with policy administration, export/import, and stronger governance.
