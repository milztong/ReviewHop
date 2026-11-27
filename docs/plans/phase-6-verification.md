# Phase-6-Verifikationsnachweis

- Datum: 2026-11-22
- Status: Abgeschlossen

## Gelieferter Scope

- atomare Erzeugung von `SyncJob` und versioniertem `SyncRequested`-Outbox-Event
- sofortige HTTP-Annahme mit `202 Accepted` statt blockierendem Providerzugriff
- eigener Spring-Boot-Worker-Prozess aus demselben Backend-Image ohne HTTP-Port
- PostgreSQL-Leasing über `FOR UPDATE SKIP LOCKED` mit zehnminütigem Lease
- Lease-Erneuerung nach jeder vollständig importierten Providerseite
- at-least-once-Redelivery nach Lease-Ablauf und idempotenter Learning-Import
- höchstens fünf automatische Versuche mit exponentiellem Backoff, Jitter und
  Beachtung eines größeren Provider-`Retry-After`
- sichtbare Zustände `REQUESTED`, `RUNNING`, `RETRY_SCHEDULED`, `SUCCEEDED`,
  `TERMINAL_FAILED` und `DEAD_LETTERED`
- rollenbasierter Operator-Retry für Dead-Letter-Aufträge; Zustellbudget wird
  bewusst zurückgesetzt
- W3C-`traceparent` und Correlation-ID im Outbox-Umschlag
- strukturierte Worker-Logs sowie technische Erfolgs-, Fehler- und Latenzmetriken
- Status-Endpunkt und Next.js-Polling für den asynchronen Benutzerfluss
- additive Flyway-Migration V7 sowie lokale und Hetzner-Compose-Worker

## Ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Vollständiger Backendlauf | `mvn verify`: 23 Tests, 0 Fehler, 0 übersprungen; separater Non-Web-Worker-Kontext enthalten |
| PostgreSQL-/Outbox-Integration | 4 Tests: atomare Annahme, Pagination, Idempotenz, Teilfehler, exklusives Lease, Redelivery, fünf Versuche, Dead Letter und Operator-Retry |
| Flyway und JPA | V1 bis V7 auf leerem PostgreSQL 17.12 erfolgreich; Hibernate-Schemavalidierung erfolgreich |
| Architektur und Format | Spring Modulith/ArchUnit erfolgreich; 0 Checkstyle-Verstöße |
| Frontend | 9 Komponententests, TypeScript und ESLint erfolgreich |
| Next.js-Produktionsbuild | Erfolgreich; neuer SyncJob-Status-BFF-Endpunkt im Route-Manifest |
| Compose | Lokale und Hetzner-Konfiguration erfolgreich validiert |
| Container-Build | Gemeinsames Backend-Image für API/Worker und Frontend-Image erfolgreich gebaut |

## Abgleich mit der Definition of Done

- Der HTTP-Request führt keinen Providerzugriff mehr aus.
- SyncJob und Outbox-Event teilen eine kurze Datenbanktransaktion; es gibt keinen
  Dual Write zu einem externen Broker.
- Claim und Zustandswechsel sind kurz; Provider-HTTP läuft außerhalb einer
  Datenbanktransaktion.
- Ein Lease-Ablauf führt zur erneuten Zustellung, nicht zum Verlust des Auftrags.
- Import-Unique-Constraints und der idempotente Learning-Port schützen vor
  doppelter Wirkung.
- Besitzergebundene Statusabfragen liefern für fremde Benutzer `404`.
- Operator-Retry benötigt `ROLE_ankileet-operator`; normale Benutzer erhalten
  `403`.
- Outbox und Logs enthalten keine Provider-Credentials, Lösungen oder
  vollständigen Aufgabenstellungen.
- PostgreSQL ist bereits Teil der lokalen und Hetzner-Topologie; Phase 6 erzeugt
  keine zusätzliche Cloudressource und keine laufenden Brokerkosten.

## CI-Bestätigung

- GitHub Actions hat Backend, Frontend, Browser-E2E, Containerbuild und die
  bestehenden Trivy-Gates für Commit `6f40be3` vollständig grün bestätigt.
- Der auf den asynchronen `202`-/Polling-Vertrag aktualisierte Playwright-Flow
  lief dabei im isolierten CI-Stack mit ephemeren OIDC-Testbenutzern und Secrets.
- Die Bestätigung wurde am 2026-11-22 durch den Repository-Eigentümer gemeldet.

## Verbleibende Risiken

- API und Worker teilen PostgreSQL als Ausfall- und Kapazitätsgrenze. Das ist für
  die erwartete Demo-Last bewusst akzeptiert.
- Verarbeitete Outbox-Events werden noch nicht automatisch bereinigt. Eine
  Aufbewahrungs- und Bereinigungsregel ist vor dem öffentlichen Release nötig.
- Der Worker besitzt noch keinen eigenen externen Health-Endpunkt. Compose
  überwacht den Prozessneustart; technische Queue- und Worker-Dashboards folgen
  in Phase 7.
- Vollständige OpenTelemetry-Span-Verknüpfung über den gespeicherten
  `traceparent` folgt in Phase 7. Phase 6 stellt Korrelation und Transport bereit.

## Verbindliche Entscheidungen

- [ADR-0001](../adr/0001-modular-monolith-and-worker.md)
- [ADR-0016](../adr/0016-codewars-sync-consistency.md)
- [ADR-0017](../adr/0017-postgresql-outbox-worker.md)
