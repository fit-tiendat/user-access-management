# User Access Management Architecture

## 1. System context

User Access Management is a Maven multi-module system with two independently
deployed Spring Boot services. Caddy is the only public application entry point
and terminates TLS before routing versioned API paths.

```mermaid
flowchart LR
    client["Web / API client"]

    subgraph public["Public edge"]
        gateway["Caddy gateway<br/>TLS + JSON access logs"]
    end

    subgraph services["Private Compose network"]
        auth["auth-service<br/>Spring Boot :8081"]
        user["user-service<br/>Spring Boot :8082"]
        authMgmt["Auth management :9081"]
        userMgmt["User management :9082"]
    end

    subgraph data["PostgreSQL instance"]
        authDb[("auth_service")]
        userDb[("user_service")]
    end

    subgraph operations["Operations"]
        prometheus["Prometheus"]
        alertmanager["Alertmanager"]
        backup["Backup worker"]
        backupVolume[("Backup volume")]
    end

    client -->|"HTTPS /api/v1/auth/*<br/>/api/v1/role/*"| gateway
    client -->|"HTTPS /api/v1/users/*"| gateway
    gateway --> auth
    gateway --> user
    auth --> authDb
    user --> userDb
    auth --- authMgmt
    user --- userMgmt
    prometheus --> authMgmt
    prometheus --> userMgmt
    prometheus --> alertmanager
    backup --> authDb
    backup --> userDb
    backup --> backupVolume
```

Public traffic cannot directly reach management ports. Service debug ports,
Prometheus, and Alertmanager bind to host loopback for local operations only.

## 2. Source modules and ownership

| Module/component | Owns |
| --- | --- |
| `core` | JWT parsing/signing, security filter, shared DTOs, exceptions, audit logger, CORS policy, OpenAPI and logging configuration |
| `auth-service` | Registration, password authentication, role checks, user credentials, JWT issuance |
| `user-service` | Profile commands/queries, profile cache, profile database |
| `gateway` | TLS termination, route mapping, security headers, access logs |
| `monitoring` | Metrics scraping, service/latency/error/heap alerts |
| `operations` | Verified PostgreSQL backups, runbook, disaster recovery |

`core` is a library, not a network service. A change to shared security code
requires rebuilding and deploying both application services.

## 3. Trust boundaries

```mermaid
flowchart TB
    internet["Untrusted network"]
    edge["TLS gateway"]
    app["Authenticated service boundary"]
    db["Database boundary"]
    ops["Operations boundary"]
    secrets["Deployment secret store"]

    internet -->|"HTTPS only"| edge
    edge -->|"Forwarded HTTP on private network"| app
    app -->|"Service-specific credentials"| db
    ops -->|"Loopback / private management ports"| app
    secrets -->|"Runtime environment only"| app
    secrets -->|"Runtime environment only"| db
```

Security rules:

- Caddy redirects HTTP to HTTPS and emits structured access logs.
- Each service owns its URL authorization rules; `JwtFilter` only validates a
  presented bearer token and builds the Spring Security context.
- JWT signing keys must contain at least 256 bits. During a planned rotation,
  both services accept the current and previous verification keys, while only
  the current key signs new tokens.
- Registration always creates `ROLE_USER`; public input cannot assign an
  administrator or moderator role.
- Passwords are BCrypt hashes in `auth_service`; raw passwords and JWT values
  must never be logged.
- CORS uses an explicit list of trusted origins.

## 4. Data architecture

The system follows database-per-service ownership even though both databases
currently run in one PostgreSQL container.

```mermaid
erDiagram
    AUTH_USERS {
        bigint id PK
        varchar username UK
        varchar password
        varchar email UK
        varchar full_name
        varchar role
        timestamp created_at
        timestamp updated_at
        varchar phone
        timestamp last_login
    }

    USER_PROFILES {
        bigint id PK
        varchar username UK
        varchar full_name
        varchar email UK
    }

    AUTH_USERS ||..o| USER_PROFILES : "logical username reference"
```

There is no cross-database foreign key. `profiles.username` is a logical
reference to the JWT subject issued for `users.username`. The user service does
not query `auth_service`; it trusts a valid JWT and uses its subject as the
profile key.

Flyway owns both schemas:

- `auth-service` migrations create and evolve `users`.
- `user-service` migrations create `profiles`. Migration V3 preserves the
  obsolete `user_profiles` table as `legacy_user_profiles` for reconciliation
  instead of deleting unknown data.
- Hibernate schema generation is disabled in production.

## 5. Registration flow

```mermaid
sequenceDiagram
    actor Client
    participant Gateway as Caddy
    participant Limit as Auth rate-limit filter
    participant Controller as AuthController
    participant Service as RegistrationService
    participant DB as auth_service
    participant Audit as Security audit log

    Client->>Gateway: POST /api/v1/auth/register
    Gateway->>Limit: Forward validated TLS request
    Limit->>Controller: Allowed request
    Controller->>Controller: Bean validation
    Controller->>Service: register(request)
    Service->>DB: existsByUsername(normalized username)
    alt username exists
        Service->>Audit: REGISTRATION_REJECTED
        Service-->>Client: 409 conflict
    else new username
        Service->>Service: BCrypt password
        Service->>DB: Save user with ROLE_USER
        Service->>Audit: REGISTRATION_SUCCESS
        Service-->>Client: 200 success
    end
```

The rate limiter is keyed by client identity and has bounded in-memory state.
Validation rejects weak passwords and malformed usernames before persistence.

## 6. Login and JWT issuance

```mermaid
sequenceDiagram
    actor Client
    participant Gateway as Caddy
    participant Limit as Auth rate-limit filter
    participant Auth as AuthenticationService
    participant Strategy as PasswordAuthenticationStrategy
    participant DB as auth_service
    participant JWT as JwtService
    participant Audit as Security audit log

    Client->>Gateway: POST /api/v1/auth/login
    Gateway->>Limit: Forward request
    Limit->>Auth: Allowed credentials
    Auth->>Strategy: Select password strategy
    Strategy->>DB: Load user and BCrypt hash
    Strategy->>Strategy: AuthenticationManager verifies password
    alt invalid credentials
        Strategy->>Audit: LOGIN_FAILURE
        Strategy-->>Client: 401 generic error
    else valid credentials
        Strategy->>JWT: Sign subject + role with current key
        Strategy->>Audit: LOGIN_SUCCESS
        Strategy-->>Client: 200 JWT
    end
```

Authentication errors are deliberately generic. Internal exception messages,
database details, credentials, and stack traces are not returned to clients.

## 7. Authenticated profile read

```mermaid
sequenceDiagram
    actor Client
    participant Gateway as Caddy
    participant Filter as JwtFilter
    participant Security as Spring Security
    participant Controller as ProfileController
    participant Cache as Caffeine profile cache
    participant DB as user_service

    Client->>Gateway: GET /api/v1/users/me + Bearer JWT
    Gateway->>Filter: Forward private-network request
    Filter->>Filter: Verify signature and expiry
    Filter->>Security: Set username + role authorities
    Security->>Controller: Authorize request
    Controller->>Cache: Lookup by JWT subject
    alt cache hit
        Cache-->>Controller: Profile
    else cache miss
        Cache->>DB: SELECT profile by username
        DB-->>Cache: Profile
        Cache-->>Controller: Profile
    end
    Controller-->>Client: Standard ApiResponse
```

The profile cache is local to each user-service process, bounded by maximum
entries and expiry time. Successful upsert and delete operations evict the
affected username so later reads cannot return a known stale value. JWT
validation results are intentionally not cached.

## 8. Availability and operations

- Docker health checks use Actuator readiness endpoints on private management
  ports.
- Prometheus scrapes both services and evaluates availability, 5xx rate, p95
  latency, and JVM heap pressure.
- Alertmanager groups alerts. A production deployment must add an external
  notification receiver.
- The backup worker creates custom-format dumps for both databases, validates
  them with `pg_restore --list`, writes SHA-256 checksums, and enforces
  retention.
- Caddy, service containers, PostgreSQL, monitoring, and backup worker have
  explicit CPU, memory, and process limits.

Operational procedures are defined in:

- `operations/RUNBOOK.md`
- `operations/DISASTER_RECOVERY.md`

## 9. Deployment and verification path

```mermaid
flowchart LR
    commit["Reviewed commit"] --> ci["CI: tests + JaCoCo"]
    commit --> security["CodeQL + dependency review"]
    ci --> images["Non-root service images"]
    security --> images
    images --> compose["Docker Compose deployment"]
    compose --> health["Readiness + HTTPS smoke tests"]
    health --> perf["Manual k6 thresholds"]
    perf --> operate["Prometheus + Alertmanager + backups"]
```

The performance workflow is manual because it targets a deployed HTTPS
environment and requires a dedicated test account from the GitHub `performance`
environment secrets.

## 10. Architectural constraints

- Both services must use the same current JWT key and key-rotation window.
- Database ownership must not be bypassed with cross-service table access.
- Administrative access belongs to service security rules and method
  authorization; it must not be inferred from request input.
- List endpoints must remain paginated and bounded.
- Cache entries must be bounded and invalidated on writes.
- New public endpoints must use `/api/v1`, OpenAPI annotations, validation,
  authorization rules, and standard exception responses.
- New long-running or retryable background work should use a durable queue or
  scheduler. `@Async` alone is not a durability guarantee.
