## Load testing (Gatling)

This folder is intentionally **not** part of the Maven build. It’s a lightweight, runnable example for validating:
- throughput (target 10k req/s in a real environment)
- latency overhead
- correctness under contention (same key)

### Prereqs
- Docker running (for `docker compose up`)
- A Gatling distribution (or you can copy the simulation into your preferred Gatling setup)

### Run stack

```bash
docker compose up -d redis postgres app
```

### Simulations

- `RateLimitSmokeSimulation.scala`: basic steady-state and burst traffic patterns

