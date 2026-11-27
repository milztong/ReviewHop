# Offene Entscheidungen

Diese Liste enthält Punkte, die noch nicht verbindlich als ADR akzeptiert wurden. Empfohlene Defaults ermöglichen weitere Planung, ersetzen aber keine Produktentscheidung.

| ID | Entscheidung | Empfohlener Default | Benötigt vor |
|---|---|---|---|
| PD-007 | Benötigt Version 1 Erinnerungsbenachrichtigungen? | Nein; Due-Liste in der Anwendung genügt. | Releaseplanung |
| PD-008 | Wird Version 1 als PWA angeboten? | Responsive Web-App zuerst; PWA nur bei niedrigem Zusatzaufwand. | Phase 7 |
| TD-003 | Wann werden die DNS-Einträge für ReviewHop eingerichtet? | `reviewhop.tongmilz.com` ist vorgesehen. Bestehende DNS-Einträge prüfen und nach Bereitstellung der Hetzner-IPv4 konfigurieren. | Vor öffentlichem Hetzner-Deployment |
| TD-004 | Welche AWS-Region wird verwendet? | `eu-central-1`, sofern Kosten und Dienstverfügbarkeit passen. | Vor optionalem AWS-Deployment |
| TD-005 | Welches monatliche Kostenlimit gilt für die Hetzner-Demo? | Aktuellen Preis für Server, IPv4, Backups und Backupziel vor Provisionierung freigeben. | Vor Hetzner-Apply |
| TD-006 | Wird das Repository öffentlich und unter welcher Lizenz? | Öffentlich nach Secret- und Lizenzprüfung; Apache-2.0 oder MIT prüfen. | Vor erstem Push |
| TD-007 | Wo laufen Prometheus, Grafana, Loki und Tempo in der Demo? | **Entschieden:** lokal vollständig; auf Hetzner optional auf demselben Server nach Ressourcenfreigabe, ohne öffentlichen Grafana-Zugang. Siehe [ADR-0018](../adr/0018-self-hosted-observability-stack.md). | Erledigt 2026-11-23 |
| TD-014 | Wohin werden tägliche verschlüsselte PostgreSQL-Backups geschrieben und wie lange aufbewahrt? | **Entschieden:** privater Hetzner-Object-Storage-Bucket in einer vom Server getrennten deutschen Region; Restic-Retention 7 täglich, 4 wöchentlich, 3 monatlich. Siehe [ADR-0020](../adr/0020-encrypted-postgresql-backups-with-restic.md). | Erledigt 2026-11-24; Kostenfreigabe und Apply bleiben offen |
| TD-015 | Welches verschlüsselte Remote Backend ersetzt lokalen Terraform-State bei Zusammenarbeit? | Vor einer zweiten schreibenden Person oder automatischem Infrastruktur-Apply entscheiden. | Vor Infrastruktur-Zusammenarbeit |

## Entscheidungsprozess

1. Produktentscheidungen erhalten eine Ergänzung der Produktvision oder ein eigenes Produktentscheidungsdokument.
2. Architekturentscheidungen werden als ADR festgehalten.
3. Entscheidungen mit Cloudkosten oder externen Veröffentlichungen benötigen explizite Benutzerfreigabe.
4. Erledigte Punkte werden nicht gelöscht, sondern mit Ergebnis und Verweis auf die verbindliche Quelle markiert.

## Entschiedene Punkte

| ID | Ergebnis | Verbindliche Quelle |
|---|---|---|
| TD-002 | Next.js läuft als Node.js-Server im Standalone-Modus, nicht als statischer Export. | [ADR-0008](../adr/0008-nextjs-bff.md) |
| TD-003 (teilweise) | Die Java Group ID lautet `dev.reviewhop`. Vorgesehen sind `reviewhop.tongmilz.com`, `auth.reviewhop.tongmilz.com` und `auth-admin.reviewhop.tongmilz.com`. Die DNS-Konfiguration steht aus. | [ADR-0022](../adr/0022-reviewhop-brand-and-compatible-identifiers.md) |
| TD-008 | Maven ist das Java-Build-Tool. | `backend/pom.xml` |
| TD-009 | npm mit committed `package-lock.json` ist der JavaScript-Paketmanager. | `frontend/package.json` |
| TD-010 (Architekturrichtung) | Browserzugriffe laufen über einen BFF; OIDC-Sessions werden in Phase 3 mit sicheren HttpOnly-Cookies konkretisiert. | [ADR-0008](../adr/0008-nextjs-bff.md) |
| Cloud-Betriebsmodell | Hetzner Cloud ist die primäre öffentliche Demo; lokal bleibt der Entwicklungsstandard, AWS ist optional. | [ADR-0012](../adr/0012-hetzner-primary-demo-platform.md) |
| PD-001, PD-002, PD-006 | Eine automatische `APPROACH_RECALL`-Karte; Rating verpflichtend, Dauer automatisch, Notiz optional; nur minimale fremde Metadaten. | [Phase-3-Produktentscheidungen](phase-3-decisions.md) |
| TD-011 | Flyway-Migrator und laufende Anwendung verwenden getrennte PostgreSQL-Rollen. | [ADR-0014](../adr/0014-separate-database-runtime-roles.md) |
| TD-012 | Keycloak ist der lokale und primäre Hetzner-OIDC-Provider; Auth.js hält die Browser-Session im BFF. | [ADR-0013](../adr/0013-keycloak-oidc-and-bff-session.md) |
| PD-003, PD-004 | Vorgezogene Reviews sind erlaubt; die initiale gewünschte Retention beträgt pro Benutzer 0,90. | [Phase-4-Produktentscheidungen](phase-4-decisions.md) |
| TD-001 | Java-FSRS 1.0.0 implementiert FSRS-6 hinter dem ReviewHop-Port mit versionierten Parametern. | [ADR-0015](../adr/0015-java-fsrs-6-adapter.md) |
| PD-005 | Codewars-Benutzername als unbestätigte öffentliche Verbindung; keine Behauptung der Account-Inhaberschaft und keine Credentials. | [Phase-5-Produktentscheidungen](phase-5-decisions.md) |
| PD-009 | Versionierter JSON-Export aller eigenen Fachdaten und idempotente vollständige Fachdatenlöschung; der externe OIDC-Account bleibt getrennt. | [Phase-8-Produktentscheidungen](phase-8-decisions.md) |
| TD-013 | PostgreSQL dient lokal und auf Hetzner als persistenter Outbox-Transport mit separatem Worker, Leasing, Retry und Dead Letter; SQS bleibt optional. | [ADR-0017](../adr/0017-postgresql-outbox-worker.md) |
| TD-014 | Restic sichert ReviewHop und Keycloak täglich verschlüsselt in privaten Hetzner Object Storage außerhalb der Serverregion; Retention 7 täglich, 4 wöchentlich, 3 monatlich. | [ADR-0020](../adr/0020-encrypted-postgresql-backups-with-restic.md) |
