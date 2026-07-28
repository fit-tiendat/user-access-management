# User Access Management Disaster Recovery Plan

This plan covers loss or corruption of the User Access Management application,
its two PostgreSQL databases, the Docker host, or deployment secrets. It must be
used together with `operations/RUNBOOK.md`.

## 1. Recovery objectives

The following are initial engineering targets and must be approved by the
service owner before they are treated as contractual objectives:

| Objective | Initial target | Basis |
| --- | --- | --- |
| Recovery point objective (RPO) | 24 hours | Default backup interval is 86,400 seconds |
| Recovery time objective (RTO) | 2 hours | Restore, validation, cutover, and smoke tests |
| Backup retention | 14 days | Default local retention; off-host policy may be longer |

Reduce `BACKUP_INTERVAL_SECONDS` if a 24-hour RPO is not acceptable. The backup
schedule, measured restore time, database size, and off-host transfer time must
be reviewed after every recovery exercise.

## 2. Recovery scope

The recoverable system consists of:

- approved Git commit and CI/CD definitions
- deployment configuration with secrets stored outside Git
- `auth_service` PostgreSQL database
- `user_service` PostgreSQL database
- verified database dumps and SHA-256 sidecars
- Caddy gateway, Prometheus, and Alertmanager configuration
- notification integrations configured at deployment time

Prometheus history and Caddy certificate state are useful but not required to
restore the business service. Caddy can obtain a replacement certificate after
DNS and network access are restored.

## 3. Responsibilities

Assign named people to these roles in the production operations system:

| Role | Responsibility |
| --- | --- |
| Incident commander | Declares recovery, owns decisions and status updates |
| Database recovery operator | Selects, verifies, and restores database dumps |
| Application operator | Rebuilds services, changes datasource targets, and runs smoke tests |
| Security owner | Rotates compromised credentials and signing keys |
| Business/service owner | Confirms user-visible recovery and accepts residual risk |

One person may hold multiple roles for a small system, but a second person must
review the selected backup files and the production cutover command.

## 4. Declare a disaster

Use this plan when one or more of these conditions applies:

- the Docker host or PostgreSQL volume is permanently unavailable
- database corruption cannot be repaired safely in place
- an incorrect migration or bulk operation caused material data loss
- credentials or JWT signing keys were compromised
- the normal rollback procedure cannot restore service

Before changing state:

1. Open an incident record and record all times in UTC.
2. Stop automated deployments and nonessential writes.
3. Preserve application, gateway, PostgreSQL, and audit logs.
4. Record the current Git commit, image IDs, environment key names, and volume
   names without copying secret values into the incident.
5. Decide whether the existing host is trustworthy. For host compromise,
   recover on a newly provisioned host.

## 5. Select a recovery point

Choose an `auth_service` dump and a `user_service` dump from the same backup
cycle whenever possible. Backups are created sequentially, so they are not a
single cross-database transaction.

List candidate files:

```sh
docker compose exec -T db-backup ls -lh /backups
docker compose logs --since=15d db-backup
```

For each selected dump, verify both the checksum and PostgreSQL catalog:

```sh
docker compose exec -T db-backup \
  sha256sum -c /backups/<database_timestamp>.dump.sha256

docker compose exec -T db-backup \
  pg_restore --list /backups/<database_timestamp>.dump
```

Reject a dump if either command fails. Record the selected filenames, SHA-256
values, creation time, and the reason for choosing that recovery point.

If the Docker host is lost, retrieve the same dump and checksum pairs from the
approved off-host backup location. Never rely only on the Compose backup volume.

## 6. Provision a clean recovery target

For host loss or suspected host compromise:

1. Provision a new host from the approved base image.
2. Install the supported Docker Engine and Compose v2.
3. Check out the last approved Git commit.
4. Restore the production environment from the secret manager into a protected,
   untracked `.env`.
5. Rotate host, database, monitoring, and deployment credentials if compromise
   is possible.
6. Validate configuration with `docker compose config --quiet`.
7. Start only PostgreSQL and the backup utility:

   ```sh
   docker compose up -d postgres-db db-backup
   docker compose ps
   ```

Do not attach an old PostgreSQL data directory to a different major PostgreSQL
version. Restore logical dumps into the supported server version instead.

## 7. Restore into new databases

Do not overwrite the damaged production databases. Create recovery databases
with unique incident identifiers:

```sh
docker compose exec -T postgres-db \
  sh -c 'createdb --username="$POSTGRES_USER" auth_service_recovery_INCIDENT_ID'

docker compose exec -T postgres-db \
  sh -c 'createdb --username="$POSTGRES_USER" user_service_recovery_INCIDENT_ID'
```

Restore through the backup container, which already has the protected database
connection environment:

```sh
docker compose exec -T db-backup \
  pg_restore \
  --dbname=auth_service_recovery_INCIDENT_ID \
  --no-owner \
  --no-acl \
  --exit-on-error \
  /backups/<auth_service_timestamp>.dump

docker compose exec -T db-backup \
  pg_restore \
  --dbname=user_service_recovery_INCIDENT_ID \
  --no-owner \
  --no-acl \
  --exit-on-error \
  /backups/<user_service_timestamp>.dump
```

For off-host dumps, copy the verified files into the protected backup volume
before running these commands.

## 8. Validate restored data

Run validation before directing applications to the recovery databases.

For both databases:

- verify `flyway_schema_history` contains only successful migrations
- verify expected schemas, tables, constraints, and indexes exist
- compare table counts with the backup record or last known monitoring values
- sample recent rows and business-critical relationships
- confirm timestamps and text encoding are plausible

Example read-only checks:

```sh
docker compose exec -T postgres-db \
  sh -c 'psql --username="$POSTGRES_USER" --dbname=auth_service_recovery_INCIDENT_ID \
  --command="SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'

docker compose exec -T postgres-db \
  sh -c 'psql --username="$POSTGRES_USER" --dbname=user_service_recovery_INCIDENT_ID \
  --command="SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

The database operator and a second reviewer must sign off before cutover.

## 9. Application cutover

1. Change `SPRING_DATASOURCE_URL_AUTH` and
   `SPRING_DATASOURCE_URL_USER` to the two recovery database names.
2. If secrets may have been exposed, replace `POSTGRES_PASSWORD`,
   `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and notification credentials.
3. Build from the approved release commit:

   ```sh
   docker compose build --pull auth-service user-service
   ```

4. Start or recreate the application and gateway:

   ```sh
   docker compose up -d --force-recreate \
     auth-service user-service gateway prometheus alertmanager
   ```

5. Run all health checks in `operations/RUNBOOK.md`.
6. Run these user journeys with dedicated recovery-test accounts:

   - login and receive a JWT
   - read and update the current profile
   - confirm an unauthorized request returns 401
   - confirm a non-admin cannot list or delete profiles
   - confirm an approved admin can use the administrative operations

7. Watch error rate, latency, heap pressure, and service availability for at
   least 30 minutes before closing the recovery phase.

## 10. Abort and rollback the cutover

If validation or smoke testing fails:

1. Stop public traffic at the gateway.
2. Preserve logs and the failed recovery databases.
3. Point datasource URLs back to the last known usable databases, if they remain
   safe, and recreate both services.
4. Otherwise select the previous verified backup pair and repeat the restore
   into another set of new database names.

Do not modify `flyway_schema_history`, delete the damaged databases, or remove
the old volumes while the incident is active.

## 11. Security-specific recovery

### JWT signing key compromise

Replace `JWT_SECRET` in both services at the same time. Do not put the
compromised key into `JWT_PREVIOUS_SECRET`; that would continue accepting
compromised tokens. Recreate both services and require all users to log in
again.

### Database credential compromise

Create or rotate the PostgreSQL credential, update the deployment secret store,
update both datasource passwords, recreate the services, and verify that the old
credential no longer authenticates.

### Host compromise

Use a new host, new credentials, a trusted release commit, and backup files
verified against checksums stored outside the compromised host.

## 12. Return to normal operation

Before resolving the incident:

- confirm both services and all Prometheus targets are healthy
- confirm external Alertmanager notifications are delivered
- create and verify a fresh backup of both recovered databases
- copy the fresh backup off-host
- preserve the old databases and evidence according to incident policy
- record actual RPO, RTO, data loss, and affected users
- schedule a post-incident review with owners and deadlines

## 13. Recovery exercises

- Monthly: verify recent checksums and run `pg_restore --list`.
- Quarterly: restore both databases into new names and run smoke tests.
- After every Flyway migration: prove that the latest backup restores and the
  approved application version starts against it.
- Annually: simulate loss of the Docker host and recover from off-host storage.

Record measured duration and failures from every exercise. Update this plan,
backup interval, resource limits, and recovery objectives based on evidence.
