# UUID migration and recovery

The migration was run on PostgreSQL 16 in the local Docker Compose environment on 2026-10-06. Its backups are in `D:\code\java\springboot\BE_KhaoSat-backup-uuid-20261006`:

- `surveydb-before-uuid.dump`: complete pre-migration PostgreSQL custom archive. Use only with the old application version.
- `minio-before-uuid`: pre-migration MinIO volume snapshot.
- `surveydb-after-uuid.dump`: complete PostgreSQL custom archive after ID conversion, seed and administrator creation. This is the normal restore point for the UUID application.
- `minio-after-uuid`: object-level backup of the media bucket at the same point.
- `admin-credentials.txt`: the new local administrator username and one-time generated password. Keep this folder private.
- `SHA256SUMS.txt`: SHA-256 checksums for the 72 backup files, including database archives and media.

Both database archives were restored into separate databases successfully. The UUID restore has 395 ID mappings, 24 foreign keys, 4 surveys, 10 responses and 6 media records. The migration preserves `uuid_migration.id_map` for tracing each old numeric ID to its UUID.

## Restore the UUID snapshot later

Run from the repository directory in PowerShell. These commands **replace** `surveyDB` with the snapshot; stop application writers and take a new backup first if newer data matters.

```powershell
docker compose stop auth-service core-service survey-service notification-service gateway-service
docker compose cp 'D:\code\java\springboot\BE_KhaoSat-backup-uuid-20261006\surveydb-after-uuid.dump' postgres:/tmp/restore-uuid.dump
docker compose exec -T postgres dropdb -U postgres --force surveyDB
docker compose exec -T postgres createdb -U postgres surveyDB
docker compose exec -T postgres pg_restore -U postgres -d surveyDB --exit-on-error /tmp/restore-uuid.dump
docker compose cp 'D:\code\java\springboot\BE_KhaoSat-backup-uuid-20261006\minio-after-uuid' minio:/tmp/restore-uuid-media
docker compose exec -T minio sh -c 'mc alias set local http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null'
docker compose exec -T minio sh -c 'mc mirror --overwrite /tmp/restore-uuid-media "local/$MINIO_DEFAULT_BUCKETS"'
docker compose cp survey-service/src/main/resources/db/verify-uuid.sql postgres:/tmp/verify-uuid.sql
docker compose exec -T postgres psql -U postgres -d surveyDB -f /tmp/verify-uuid.sql
docker compose up -d auth-service core-service survey-service notification-service gateway-service
```

The MinIO restore copies backed-up objects and leaves any unrelated newer objects in the bucket. To restore the pre-migration state, use both `surveydb-before-uuid.dump` and `minio-before-uuid` with the old service images and configuration.

## SQL files

`survey-service/src/main/resources/db/migrate-ids-to-uuid.sql` converts an existing numeric-ID database once. `schema-uuid.sql` initializes an empty database. `seed-surveys-97-kh.sql` adds the sample surveys where absent. `verify-uuid.sql` checks types, mappings and relationships. All application services use Hibernate schema validation.
