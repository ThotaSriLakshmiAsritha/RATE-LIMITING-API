## Rate Limiting Service (Token Bucket + Redis)

Production-grade, distributed rate limiting demo built with **Spring Boot 3**, **Redis**, and **Token Bucket (Lua)**.

### Quickstart (local)

Start dependencies:

```bash
docker compose up -d redis postgres
```

Run the app:

```bash
./mvnw spring-boot:run
```

Or run everything in Docker:

```bash
docker compose up --build
```

Health + metrics:
- `GET /actuator/health`
- `GET /actuator/prometheus`

Swagger UI:
- `/swagger-ui.html`

### Demo auth

Login (default seeded user: `demo` / `password`):

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"demo\",\"password\":\"password\"}"
```

The app also serves a local JWK set at `/.well-known/jwks.json` for local verification.

### Supabase Auth JWTs (profile: local)

Run the service as a Supabase JWT **resource server**:

```bash
set SPRING_PROFILES_ACTIVE=local
set SUPABASE_PROJECT_REF=<your-project-ref>
./mvnw spring-boot:run
```

This config uses:
- JWKS: `https://<project-ref>.supabase.co/auth/v1/keys`
- Issuer: `https://<project-ref>.supabase.co/auth/v1`
- Audience: `authenticated` (override with `SUPABASE_AUDIENCE`)

Then call protected endpoints with:

```bash
curl -H "Authorization: Bearer <SUPABASE_ACCESS_TOKEN>" http://localhost:8080/api/v1/products
```

### Configuration

Default properties live in `src/main/resources/application.yml` and can be overridden via environment variables.

### Tests

- Unit tests run without Docker.
- Integration tests use Testcontainers and will **auto-skip** if Docker is not available.

