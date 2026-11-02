# Copilot Instructions

## Repository Overview

A proof-of-concept demonstrating OpenTelemetry (OTel) observability with Spring Boot 4.1.2 / Java 25 microservices sending telemetry directly to **Elastic Cloud's managed OTLP endpoint** (no local collector or Docker Compose setup needed).

Two independent Maven projects:
- **springboot-demo** (port 8080): Provider service with JPA/H2 backend
- **springboot-client-demo** (port 8081): Consumer/client service using Spring Cloud OpenFeign

Both use `opentelemetry-spring-boot-starter` (instrumentation BOM `2.33.1`), configured via `otel.*` properties and `OTEL_EXPORTER_OTLP_*` environment variables.

## Build & Run Commands

Each service must be built and run from its own directory. First, set environment variables:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="https://<motlp-endpoint>:443"
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=ApiKey <api-key>"
```

Then start the services:

```bash
# Provider service (port 8080)
cd springboot-demo
mvn spring-boot:run

# Consumer/client service (port 8081)
cd springboot-client-demo
mvn spring-boot:run
```

**Run tests** (from the module directory):
```bash
mvn test
mvn test -Dtest=SpringbootDemoApplicationTests
```

## Architecture

```
springboot-client-demo (8081) ──OpenFeign──► springboot-demo (8080)
        │                                           │
        └──────────── OTLP (HTTP) ─────────────────┘
                                    │
                    Elastic Cloud OTLP Endpoint
                        (traces, metrics, logs)
                            │
                     Elastic Observability
                   (APM > Traces, Dashboards)
```

## Services

### springboot-demo (Provider, port 8080)

Exposes REST endpoints:
- `GET /books` — JPA/H2-backed CRUD API for AdmBook entities
- `GET /fibo?n=<number>` — CPU-intensive Fibonacci calculation (for load testing)
- `GET /log` — Multi-level logging demonstration

### springboot-client-demo (Consumer, port 8081)

- `GET /trace-demo` — Calls `springboot-demo/books` via OpenFeign; traces the full distributed trace in Elastic Observability

**Feign instrumentation:** The `opentelemetry-spring-boot-starter` does not auto-instrument OpenFeign. `FeignTracingConfig` registers a custom `RequestInterceptor` that injects the `traceparent` header into outgoing requests, linking client and provider spans.

## Key Conventions

### Versions & Dependencies

- **Spring Boot:** 4.1.2
- **Java:** 25
- **OpenTelemetry Instrumentation BOM:** 2.33.1
- **Spring Cloud:** 2025.1.4 (client only)

Both services use the same OTel integration approach: `opentelemetry-spring-boot-starter` with `otel.*` configuration properties.

### Package Naming

- `springboot-demo`: `com.vorozco` (flat)
- `springboot-client-demo`: `com.vorozco.springboot_client_demo` (underscore in sub-package)

### Configuration

OTel configuration via `application.properties`:
- `otel.resource.attributes` — Service name and deployment environment
- `otel.logs/metrics/traces.exporter=otlp` — All telemetry types
- `otel.metric.export.interval=5000` — Metrics flush interval (ms)
- `otel.instrumentation.micrometer.enabled=true` — Micrometer bridge

OTLP endpoint and auth via environment:
```bash
OTEL_EXPORTER_OTLP_ENDPOINT=https://<motlp-endpoint>:443
OTEL_EXPORTER_OTLP_HEADERS=Authorization=ApiKey <api-key>
```

Logging configuration:
- `logging.level.root=INFO`
- `logging.level.com.vorozco=TRACE` — Verbose app logging
- `spring.main.banner-mode=off`

## Curl Smoke Tests

Test distributed traces flowing to Elastic Cloud:

```bash
# CPU load test (generates trace and metrics)
curl http://localhost:8080/fibo?n=45

# Logging test
curl http://localhost:8080/log

# Distributed trace test (requires both services running)
curl http://localhost:8081/trace-demo
```

Then inspect traces in **Elastic Observability** (APM > Traces).

## Notes

- No Docker Compose or local observability stack (Grafana, Prometheus, Tempo, Loki) is used
- All telemetry flows directly to Elastic Cloud's managed OTLP endpoint
- Each service is independently built/run; there is no parent POM
