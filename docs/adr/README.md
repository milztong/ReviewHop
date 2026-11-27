# Architecture Decision Records

ADRs dokumentieren verbindliche Architekturentscheidungen. Akzeptierte ADRs werden nicht stillschweigend umgeschrieben. Eine geänderte Entscheidung erhält ein neues ADR, das das frühere ersetzt.

## Statuswerte

- `Proposed`: zur Entscheidung vorbereitet
- `Accepted`: verbindlich
- `Deprecated`: weiterhin historisch relevant, aber nicht mehr empfohlen
- `Superseded`: durch ein neueres ADR ersetzt
- `Rejected`: erwogen und verworfen

## Index

| ADR | Entscheidung | Status |
|---|---|---|
| [0001](0001-modular-monolith-and-worker.md) | Modularer Monolith mit separatem Sync-Worker | Accepted |
| [0002](0002-clean-hexagonal-architecture.md) | Clean/Hexagonal Architecture und Package by Feature | Accepted |
| [0003](0003-provider-connector-strategy.md) | Capability-basierte Provider-Connectoren | Accepted |
| [0004](0004-fsrs-behind-scheduling-port.md) | FSRS hinter einem Scheduling-Port | Accepted |
| [0005](0005-oidc-and-cognito.md) | OIDC mit Cognito als Produktionsprovider | Superseded |
| [0006](0006-aws-and-terraform.md) | AWS als Zielplattform und Terraform als IaC | Superseded |
| [0007](0007-sqs-outbox-and-eventing.md) | SQS und Transactional Outbox vor Kafka | Superseded |
| [0008](0008-nextjs-bff.md) | Next.js als Node-Server und Backend for Frontend | Accepted |
| [0009](0009-s3-state-and-github-oidc.md) | S3 Remote State und GitHub-OIDC-Deployment | Deprecated |
| [0010](0010-aws-staging-topology.md) | Kostenbewusste AWS-Staging-Topologie | Deprecated |
| [0011](0011-zero-cost-default-and-ephemeral-aws.md) | Zero-Cost Default und kurzlebige AWS-Demos | Superseded |
| [0012](0012-hetzner-primary-demo-platform.md) | Hetzner Cloud als primäre Demo-Plattform | Accepted |
| [0013](0013-keycloak-oidc-and-bff-session.md) | Keycloak als OIDC-Provider und serverseitige BFF-Session | Accepted |
| [0014](0014-separate-database-runtime-roles.md) | Getrennte Datenbankrollen für Migration und Anwendung | Accepted |
| [0015](0015-java-fsrs-6-adapter.md) | Java-FSRS 1.0.0 als versionierter FSRS-6-Adapter | Accepted |
| [0016](0016-codewars-sync-consistency.md) | Konsistenter synchroner Codewars-Sync in Phase 5 | Accepted |
| [0017](0017-postgresql-outbox-worker.md) | PostgreSQL-Outbox als Hetzner-Arbeitswarteschlange | Accepted |
| [0018](0018-self-hosted-observability-stack.md) | Selbst gehosteter LGTM-Stack hinter OpenTelemetry | Accepted |
| [0019](0019-atomic-privacy-lifecycle.md) | Atomarer Privacy-Lebenszyklus über einen dedizierten Port | Accepted |
| [0020](0020-encrypted-postgresql-backups-with-restic.md) | Verschlüsselte PostgreSQL-Backups mit Restic und getrenntem Object Storage | Accepted |
| [0021](0021-subject-rate-limits-and-capacity-budgets.md) | Subjektbezogene API-Limits und feste Kapazitätsbudgets | Accepted |
| [0022](0022-reviewhop-brand-and-compatible-identifiers.md) | Produktname ReviewHop und technische Kompatibilität | Accepted |
