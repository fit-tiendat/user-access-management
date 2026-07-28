#!/bin/sh
set -eu

: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"

BACKUP_DATABASES="${BACKUP_DATABASES:-auth_service user_service}"
BACKUP_DIRECTORY="${BACKUP_DIRECTORY:-/backups}"
BACKUP_INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-86400}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

umask 077
mkdir -p "$BACKUP_DIRECTORY"

temporary_file=""
trap 'test -z "$temporary_file" || rm -f "$temporary_file"' EXIT INT TERM

backup_databases() {
    timestamp="$(date -u +%Y%m%dT%H%M%SZ)"

    for database in $BACKUP_DATABASES; do
        case "$database" in
            *[!a-zA-Z0-9_]*)
                echo "Invalid database name: $database" >&2
                exit 1
                ;;
        esac

        destination="${BACKUP_DIRECTORY}/${database}_${timestamp}.dump"
        temporary_file="${destination}.tmp"

        echo "Backing up ${database} to ${destination}"
        pg_dump \
            --dbname="$database" \
            --format=custom \
            --no-owner \
            --no-acl \
            --file="$temporary_file"

        pg_restore --list "$temporary_file" >/dev/null
        mv "$temporary_file" "$destination"
        temporary_file=""
        sha256sum "$destination" >"${destination}.sha256"
    done

    find "$BACKUP_DIRECTORY" \
        -type f \
        \( -name '*.dump' -o -name '*.dump.sha256' \) \
        -mtime "+${BACKUP_RETENTION_DAYS}" \
        -delete
}

while :; do
    backup_databases

    if [ "$BACKUP_INTERVAL_SECONDS" -le 0 ]; then
        break
    fi

    sleep "$BACKUP_INTERVAL_SECONDS"
done
