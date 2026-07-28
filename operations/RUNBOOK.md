# User Access Management Operations Runbook

This runbook covers the Docker Compose deployment defined in
`docker-compose.yaml`. Commands are run from the repository root.

## 1. Required access and tools

- Docker Engine with Compose v2
- Read access to the deployment repository and the approved release commit
- Access to the deployment secret store
- Access to the host that owns the Compose project and its Docker volumes
- An HTTPS client for smoke tests

Never copy production credentials into the repository, CI variables committed
to Git, issue comments, or terminal transcripts. `.env.example` documents the
expected keys; the real `.env` must remain untracked and readable only by the
deployment account.

## 2. Pre-deployment checks

1. Confirm the release commit and maintenance window.
2. Confirm a recent verified backup exists for both `auth_service` and
   `user_service`.
3. Confirm these required values are supplied by the deployment secret store:
   `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`.
4. Confirm `JWT_SECRET` is a base64-encoded key containing at least 256 bits.
5. Set `PUBLIC_DOMAIN` and `CORS_ALLOWED_ORIGINS` to the production HTTPS
   origins. Do not use wildcard origins.
6. Configure a real email, webhook, Slack, PagerDuty, or equivalent receiver in
   `monitoring/alertmanager.yml`. The repository receiver keeps alerts visible
   in Alertmanager but does not notify an external on-call system.
7. Validate the resolved Compose configuration:

   ```sh
   docker compose config --quiet
   ```

8. Run the release verification suite:

   ```sh
   ./mvnw clean verify
   ```

## 3. Deploy

Build from the approved commit and recreate changed services:

```sh
docker compose build --pull
docker compose up -d --remove-orphans
docker compose ps
```

Do not run `docker compose down --volumes` during a normal deployment. It
deletes the database and operational data volumes.

Wait until PostgreSQL, `auth-service`, and `user-service` report `healthy`.
Then check readiness from inside each service:

```sh
docker compose exec -T auth-service \
  curl --fail --silent http://localhost:9081/actuator/health/readiness

docker compose exec -T user-service \
  curl --fail --silent http://localhost:9082/actuator/health/readiness
```

Verify the public gateway over HTTPS:

```sh
curl --fail --show-error --silent \
  "https://${PUBLIC_DOMAIN}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  --data '{"username":"deployment-smoke-user","password":"<from-secret-store>"}'
```

Use a dedicated, least-privileged smoke-test user. Never put its password into
this file or shell history.

## 4. Service endpoints

| Component | Network exposure | Purpose |
| --- | --- | --- |
| Caddy gateway | Public ports 80/443 | TLS termination and API routing |
| Auth debug port | Host loopback only | Local diagnosis of auth-service |
| User debug port | Host loopback only | Local diagnosis of user-service |
| Prometheus | Host loopback only | Metrics queries and active alerts |
| Alertmanager | Host loopback only | Alert grouping and notification status |
| Management ports 9081/9082 | Compose network only | Health and Prometheus metrics |

The public API prefixes are:

- `/api/v1/auth/*` and `/api/v1/role/*` -> `auth-service`
- `/api/v1/users/*` -> `user-service`

## 5. Monitoring and alert response

Prometheus scrapes both services every 15 seconds. Check targets and active
alerts through the loopback Prometheus port. Check notification delivery
through the loopback Alertmanager port.

The supplied alerts require these responses:

| Alert | First response |
| --- | --- |
| `UamServiceDown` | Check `docker compose ps`, then service and PostgreSQL logs |
| `UamHighServerErrorRate` | Inspect recent 5xx gateway/service logs and database health |
| `UamHighRequestLatency` | Check database connections, query load, CPU, memory, and request rate |
| `UamJvmHeapPressure` | Capture JVM metrics, check traffic growth, and restart only after preserving evidence |

Useful commands:

```sh
docker compose logs --since=15m auth-service
docker compose logs --since=15m user-service
docker compose logs --since=15m gateway
docker compose stats --no-stream
```

Caddy emits structured JSON access logs containing request path, status, and
duration. Application logs record security events without logging passwords or
JWT values.

## 6. Backup verification

The `db-backup` service creates PostgreSQL custom-format dumps, validates their
catalog with `pg_restore --list`, writes SHA-256 sidecars, and removes files
older than the configured retention period.

Check that both databases have a recent dump:

```sh
docker compose exec -T db-backup ls -lh /backups
docker compose logs --since=26h db-backup
```

Verify a selected dump before relying on it:

```sh
docker compose exec -T db-backup \
  sha256sum -c /backups/<database_timestamp>.dump.sha256

docker compose exec -T db-backup \
  pg_restore --list /backups/<database_timestamp>.dump
```

Copy verified backups to storage outside the Docker host. A Compose volume on
the same host is not sufficient protection from host or disk loss.

## 7. JWT signing-key rotation

1. Generate a new base64-encoded 256-bit or stronger key in the secret manager.
2. Set the current key as `JWT_PREVIOUS_SECRET`.
3. Set the new key as `JWT_SECRET`.
4. Recreate both services together:

   ```sh
   docker compose up -d --no-deps --force-recreate auth-service user-service
   ```

5. Verify readiness, login, and a protected profile request.
6. After the maximum old-token lifetime has elapsed, clear
   `JWT_PREVIOUS_SECRET` and recreate both services again.

If only one service is rotated, tokens can be accepted by one service and
rejected by the other.

## 8. Rollback

Rollback application code only after deciding whether the release included a
database migration.

1. Preserve logs and record the failing commit.
2. If migrations are backward compatible, check out the previous approved
   commit, rebuild the two application images, and run:

   ```sh
   docker compose up -d --no-deps --build auth-service user-service gateway
   ```

3. Repeat readiness and authenticated smoke tests.
4. If the migration is not backward compatible, do not manually edit the
   Flyway history table. Follow `operations/DISASTER_RECOVERY.md` and restore
   both databases into new volumes from verified backups.

## 9. Troubleshooting

### A service remains unhealthy

```sh
docker compose ps
docker compose logs --tail=200 <service>
docker compose exec -T <service> \
  curl --verbose http://localhost:<management-port>/actuator/health/readiness
```

Check Flyway validation, database credentials, JDBC URLs, JWT key length, and
memory limits in that order.

### PostgreSQL port is already allocated

Change `PG_PORT_HOST` in the deployment environment. Applications communicate
with `postgres-db:5432` inside Compose and do not depend on the host port.

### Login works but a protected request returns 401

- Confirm both services use the same `JWT_SECRET` and `JWT_PREVIOUS_SECRET`.
- Confirm the token has not expired.
- Confirm the client sends exactly `Authorization: Bearer <token>`.
- Check security audit logs for `AUTHENTICATION_FAILURE`.

### Browser requests fail CORS

Set `CORS_ALLOWED_ORIGINS` to the exact HTTPS origins, separated by commas, and
recreate both services. Do not solve this with a wildcard origin.

### Alerts are visible but nobody is notified

The default receiver has no external integration. Add and test the deployment's
approved notification receiver in `monitoring/alertmanager.yml`, then reload or
recreate Alertmanager.

## 10. Incident records

For every production incident, record:

- start/end time in UTC and affected endpoints
- release commit and configuration changes
- alerts, relevant log correlation data, and impact
- mitigation, recovery steps, and verification evidence
- follow-up owner and deadline

Do not attach passwords, JWTs, private keys, raw `.env` files, or database dumps
to the incident record.
