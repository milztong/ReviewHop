# Roadmap

Die Roadmap ist outcome-orientiert. Phasen haben keine festen Kalenderdaten, sondern überprüfbare Exit-Kriterien.

## Status

| Phase | Ergebnis | Status |
|---|---|---|
| 0 | Architekturvertrag | **Abgeschlossen** |
| 1 | Lokaler Walking Skeleton | **Abgeschlossen** |
| 2 | Hetzner Walking Skeleton | **Aktiv – lokal verifiziert; Apply, externer Restore- und HTTPS-Nachweis offen** |
| 3 | Sicherer manueller Lern- und Review-Flow | **Aktiv – kritischer Flow, CI und IdP-Hardening lokal verifiziert; Go-live-Freigaben offen** |
| 4 | FSRS Scheduling | **Abgeschlossen** |
| 5 | Codewars-Integration | **Abgeschlossen** |
| 6 | Asynchrone Synchronisation | **Abgeschlossen** |
| 7 | Observability und UX-Reife | **Abgeschlossen** |
| 8 | Hardening und Portfolio-Release | **In Arbeit – Security-Baseline und Release-Gates** |

Ein Phasenwechsel wird in dieser Tabelle bewusst vorgenommen. Der Abschluss einer Phase erlaubt nicht automatisch unbegrenzte Arbeit an späteren Phasen.

## Phase 0 – Architekturvertrag

### Ziel

Produktumfang, Fachsprache, Architekturgrenzen, Qualitätsregeln und offene Entscheidungen sind nachvollziehbar dokumentiert.

### Ergebnisse

- Repository und Agentenregeln
- Produktvision und Glossar
- offene Produkt- und Technikentscheidungen
- C4-Systemkontext, Container- und Komponentenmodell
- Domänenmodell
- erste akzeptierte ADRs
- allgemeine Definition of Done

### Exit-Kriterien

- Dokumentlinks und Begriffe sind konsistent.
- Akzeptierte ADRs widersprechen einander nicht.
- Offene Entscheidungen sind mit spätestem Entscheidungszeitpunkt dokumentiert.
- Es existiert kein Anwendungscode.

## Phase 1 – Lokaler Walking Skeleton

### Ziel

Der kleinste vertikale technische Pfad läuft lokal und in CI.

### Scope

- Java-21-/Spring-Boot-Backend
- Next.js-/TypeScript-Frontend
- PostgreSQL, Flyway und Docker Compose
- Health-/Readiness-Endpunkte
- initiale OpenAPI-Spezifikation
- GitHub Actions für Build, Tests, Lint und Architekturchecks
- JUnit 5, Testcontainers, ArchUnit und Spring Modulith

### Exit-Kriterium

Ein Browser-Aufruf erreicht das Backend, das eine getestete PostgreSQL-Verbindung besitzt; alle Checks laufen reproduzierbar in CI.

### Aktueller Nachweis

Der lokale Vertical Slice, alle Qualitätsprüfungen, Container-Builds und die Desktop-/Mobile-Browserprüfung sind erfolgreich. [GitHub Actions Run 44766950644](https://github.com/milztong/AnkiLeet/actions/runs/44766950644) bestätigt die Gates für Commit `a8d034b` auf echten GitHub-Runnern. Phase 1 ist damit abgeschlossen. Details stehen im [Phase-1-Verifikationsnachweis](phase-1-verification.md).

## Phase 2 – Hetzner Walking Skeleton

### Ziel

Der Walking Skeleton wird kostengünstig, reproduzierbar und dauerhaft erreichbar auf Hetzner Cloud bereitgestellt.

### Scope

- Terraform für Hetzner Server, Primary IP, SSH Public Key und Cloud Firewall
- Docker Compose mit Caddy, Next.js, Spring Boot und PostgreSQL
- automatisches HTTPS und ausschließlich Caddy als öffentlicher Anwendungseingang
- GitHub Actions mit geschütztem SSH-Deployment und unveränderlichen GHCR-Images
- Server-Backup, PostgreSQL-Backup-/Restore-Konzept und Kostenfreigabe
- getrennte lokale und öffentliche Demo-Konfiguration
- vorhandenes AWS-Deployment als optionale, nicht abnahmebestimmende Referenz

### Exit-Kriterium

Ein freigegebener CI/CD-Workflow kann dieselben Container auf der gehärteten Hetzner-Demo deployen und den öffentlichen HTTPS-BFF per Smoke-Test prüfen; Backup und Restore sind dokumentiert und getestet.

### Aktueller Stand

Das Hetzner-Root-Modul, Cloud-init-Härtung, das öffentliche Compose/Caddy-Setup und der manuelle GitHub-Deploymentworkflow sind implementiert. Terraform-Formatierung/-Validierung, gemockte Guardrail-Tests, TFLint, Trivy, Actionlint sowie Compose- und Caddy-Validierung sind lokal erfolgreich. Die bisherigen AWS-Module wurden unter `infrastructure/aws` als optionale Referenz eingeordnet und nach der Verschiebung erneut validiert.

Noch offen sind das GitHub Environment, Hetzner-Projekt und API Token,
administrative Quell-CIDR, Domain/DNS, aktuelle Monatskostenschätzung,
Object-Storage-Apply und realer externer Restore-Drill sowie die ausdrückliche
Apply-Freigabe. Der Backup-/Restore-Vertrag ist lokal automatisiert verifiziert;
bislang wurden keine Cloud-Ressourcen erzeugt. Die Nachweise stehen im
[Phase-2-Verifikationsnachweis](phase-2-verification.md).

## Phase 3 – Sicherer manueller Lern- und Review-Flow

### Ziel

Ein angemeldeter Benutzer kann ohne externe Plattform einen vollständigen Review-Zyklus durchführen.

### Scope

- OIDC mit austauschbarem lokalem und öffentlichem Provider
- Benutzerprofil und Objektberechtigungen
- manuelle `SourceItem`- und `LearningItem`-Erfassung
- Aktivierung einer Review Card
- Due-Liste, Review-Abschluss und Review-Historie
- zunächst deterministische einfache SchedulingPolicy als austauschbarer Test-Double, nicht als endgültiger Algorithmus
- OpenAPI und minimale responsive Oberfläche

### Exit-Kriterium

Der kritische Benutzerweg ist per End-to-End-Test abgedeckt; ein Benutzer kann niemals Daten eines anderen Benutzers lesen oder verändern.

### Aktueller Stand

Der lokale Vertical Slice enthält Keycloak-Konfiguration, Auth.js-BFF-Session, Spring Security Resource Server, automatische `AppUser`-Zuordnung, manuelle Lerninhalte, Due-Liste, idempotenten Review-Abschluss, eine benutzergebundene Ansicht der letzten ReviewLogs und eine deterministische vorläufige `SchedulingPolicy`. Backend-Integrationstests modellieren zwei OIDC-Benutzer und den verbotenen benutzerübergreifenden Zugriff; der vollständige PostgreSQL-Testlauf über Testcontainers ist erfolgreich. Ein Playwright-E2E prüft zusätzlich mit zwei echten Keycloak-Benutzern Login, Erstellen, Review, History und Isolation. Der isolierte GitHub-Actions-Job mit kurzlebigen Secrets und Testbenutzern ist in Run 44766950644 erfolgreich. Die Hetzner-Konfiguration startet Keycloak im Produktionsmodus mit eigener Datenbank, festem Issuer, internem Health-Port und CIDR-geschütztem Admin-Host; Dump und Restore wurden isoliert verifiziert. Das externe verschlüsselte Backupziel und seine Retention sind inzwischen entschieden und lokal automatisiert getestet. Offen sind reale Go-live-Werte, Object-Storage-Apply und externer Restore-Drill sowie der bestätigte Runtime-Fix. Details stehen im [Phase-3-Verifikationsnachweis](phase-3-verification.md).

## Phase 4 – FSRS Scheduling

### Ziel

Reviews werden durch eine verifizierte, versionierte FSRS-Strategie geplant.

### Scope

- FSRS-Adapter hinter `SchedulingPolicy`
- Referenz- und Eigenschaftstests
- gewünschte Retention
- Zeit-, Zeitzonen- und vorgezogene Review-Regeln
- optimistisches Locking und Idempotency Keys
- Speicherung der Scheduler-Version im Review-Protokoll

### Exit-Kriterium

Referenzfälle liefern reproduzierbare Ergebnisse; doppelte Requests erzeugen genau einen ReviewLog-Eintrag.

### Ergebnis

Java-FSRS 1.0.0 läuft als deterministischer FSRS-6-Adapter hinter dem
`SchedulingPolicy`-Port. Review Cards speichern den aktuellen Memory State;
unveränderliche ReviewLogs speichern Version, gewünschte Retention sowie den
Zustand vor und nach dem Review. Referenz-, Eigenschafts-, Architektur- und
PostgreSQL-Integrationstests einschließlich Idempotenz bestehen. Details stehen
im [Phase-4-Verifikationsnachweis](phase-4-verification.md).

Der erste Phase-4-CI-Lauf bestätigte Backend, Frontend, Terraform und Browser-E2E,
deckte im neu erweiterten Java-Library-Scan jedoch 14 Abhängigkeitsfunde auf. Die
direkt behebbaren Bibliotheken wurden aktualisiert; zwei nicht erreichbare
Spring-MVC-Pfade sind eng begrenzt und befristet dokumentiert. Der korrigierte
[GitHub-Actions-Lauf 44914182829](https://github.com/milztong/AnkiLeet/actions/runs/44914182829)
bestätigt für Commit `81bcbb3` alle fünf Jobs einschließlich Browser-E2E und
Container-Scans. Der Backend-Scan meldet 0 unbehandelte Funde für Alpine und Java.
Damit ist das Phase-4-Exit-Kriterium erfüllt.

## Phase 5 – Codewars-Integration

**Status:** Abgeschlossen (2026-11-22)

### Ziel

Ein öffentliches Codewars-Profil kann regelkonform und idempotent importiert werden.

### Scope

- `ProviderConnector`-Port und Capabilities
- Codewars-Adapter
- Pagination und Sync-Cursor
- Provider-Verbindung und manueller Sync
- WireMock-Vertragstests
- Timeouts, Retry-Klassifizierung und Rate-Limit-Verhalten

### Exit-Kriterium

Mehrfacher Import erzeugt keine Duplikate; Fehler sind sichtbar und lassen bestehende Daten konsistent.

### Umsetzungsstand

- PD-005 ist als öffentliche, unverifizierte Profilzuordnung entschieden.
- Provider-Connector, Capability-Katalog, Codewars-HTTP-Adapter, ProviderConnection,
  synchroner SyncJob und additive Flyway-Migration V6 sind implementiert.
- Der vollständige Exit wird erst nach Gesamtprüfung und dokumentiertem
  Phase-5-Verifikationsnachweis als abgeschlossen markiert.

### Lokales Ergebnis

Der capability-basierte Codewars-Connector, öffentliche unverifizierte
Profilverbindungen, vollständige Pagination, idempotente Learning-Imports,
persistierte SyncJobs, Cursor- und Fehlerverhalten sowie die Dashboard-Integration
sind umgesetzt. Backend-Verify (21 Tests), Frontendtests (9 Tests), Typprüfung,
Lint, Produktions- und Containerbuilds bestehen. Der erweiterte Browser-E2E-Flow
und die Container-Security-Gates werden nach dem Push durch GitHub Actions
bestätigt. Der GitHub-Actions-Lauf für Commit `6af04c5` wurde am 2026-11-22
vollständig grün bestätigt. Details stehen im
[Phase-5-Verifikationsnachweis](phase-5-verification.md).

## Phase 6 – Asynchrone Synchronisation

**Status:** Abgeschlossen (2026-11-22)

### Ziel

Provider-Synchronisationen blockieren keine Benutzerrequests und sind betrieblich nachverfolgbar.

### Scope

- Transactional Outbox
- per ADR festgelegte Queue und Dead-Letter-Behandlung
- separater Spring-Boot-Worker
- PostgreSQL-Outbox-Poller für fällige Aufträge und Retries; EventBridge bleibt
  optional für ein späteres AWS-Referenz-Deployment
- at-least-once-sichere Consumer
- administrative Retry-Möglichkeit

### Exit-Kriterium

Ein Sync lässt sich vom Request über Queue und Worker bis zum Ergebnis verfolgen; wiederholte Zustellung bleibt korrekt.

### Lokales Ergebnis

`SyncJob` und `SyncRequested`-Outbox-Event entstehen atomar. Ein eigener
Spring-Boot-Worker-Prozess least PostgreSQL-Events mit `FOR UPDATE SKIP LOCKED`,
führt Providerzugriffe außerhalb der Transaktion aus und verarbeitet
Lease-Ablauf, fünf begrenzte Versuche mit Backoff/Jitter, `Retry-After`, Dead
Letter und rollenbasierten Operator-Retry. Die Next.js-Oberfläche verfolgt den
asynchronen Status. Migration V7, 23 Backendtests, 9 Frontendtests, Architektur,
OpenAPI, Produktionsbuild und Compose-Konfiguration sind lokal verifiziert.
Details stehen im [Phase-6-Verifikationsnachweis](phase-6-verification.md).

GitHub Actions bestätigte für Commit `6f40be3` Backend, Frontend, Browser-E2E,
Containerbau und die bestehenden Security-Gates vollständig grün. Damit ist das
Exit-Kriterium von Phase 6 erfüllt.

## Phase 7 – Observability und UX-Reife

**Status:** Abgeschlossen (2026-11-23)

### Ziel

Kritische Abläufe sind verständlich, messbar und benutzbar.

### Scope

- OpenTelemetry-Ende-zu-Ende-Traces
- Prometheus-Metriken
- strukturierte Logs und Loki
- Tempo und Grafana-Dashboards
- technische und fachliche Alerts
- Accessibility-, Responsive- und Browser-Prüfungen
- UX für leere, ladende und fehlerhafte Zustände

### Exit-Kriterium

Ein absichtlich ausgelöster Sync-Fehler kann anhand eines Traces und Dashboards diagnostiziert werden; zentrale Benutzerwege erfüllen die vereinbarten Accessibility-Kriterien.

### Aktueller Stand

Der lokale, kostenfreie LGTM-Stack, die OTLP-Instrumentierung des asynchronen
Sync-Pfads, Dashboard/Alerts sowie Desktop- und Mobile-Accessibility-Tests sind
implementiert. Alle sieben Observability-Dienste liefen mit den festgelegten
Images; ein synthetischer Trace wurde bis Tempo und eine gelabelte Logzeile bis
Loki nachgewiesen. Das vollständige Backend-Verify sowie acht Browser-Flows in
Desktop- und Mobile-Chromium sind lokal grün. Providerfehler, Retry und Dead
Letter sind auf Anwendungsebene getestet; die zugehörigen Trace-, Metrik- und
Logpipelines sowie das Dashboard sind im echten Observability-Stack verifiziert.
GitHub Actions wurde für Phase-7-Commit `a1eb890` vollständig grün bestätigt.
Damit ist das Exit-Kriterium erfüllt. Details stehen im
[Phase-7-Verifikationsnachweis](phase-7-verification.md).

## Phase 8 – Hardening und Portfolio-Release

**Status:** In Arbeit (seit 2026-11-24)

### Ziel

ReviewHop ist als sichere, nachvollziehbare Demo und als Bewerbungsprojekt veröffentlichbar.

### Scope

- Threat Model und Security Review
- Dependency-, Container- und Terraform-Scans
- Backup- und Restore-Test
- Last- und Resilienztests für kritische Endpunkte
- Kontolöschung und Datenexport
- Demo-Daten und Onboarding
- Architektur- und Betriebsdokumentation
- Screenshots, Live-Demo und Lebenslaufbeschreibung

### Exit-Kriterium

Die Release-Definition-of-Done ist erfüllt, kritische Findings sind geschlossen und die Demo kann aus dokumentierten Quellen reproduziert werden.

### Aktueller Stand

Das initiale Threat Model und Security Review erfassen Assets,
Vertrauensgrenzen, Angriffswege, bestehende Kontrollen und priorisierte
Restarbeiten. CI erhält einen blockierenden Repository-Secret-Scan; das
Frontend prüft seine vollständige npm-Abhängigkeitsstruktur auf hohe und
kritische bekannte Schwachstellen. Der versionierte Datenexport und die
atomare, idempotente Kontolöschung sind lokal implementiert und gegen zwei
Benutzer mit PostgreSQL und auf Ebene der Browseroberfläche verifiziert.
Backup/Restore und Wiederanlauf sind lokal automatisiert verifiziert; der reale
Object-Storage-Drill bleibt von Kosten- und Apply-Freigabe abhängig.
Subjektbezogene API-Limits, Kapazitätsbudgets, Konkurrenz- und Providerausfall-
Verträge sind implementiert und lokal verifiziert. Für den Phase-8-Push
`f4ef4e3` hat der Repository-Inhaber grüne CI gemeldet. Der
[Portfolio-Rundgang](../demo/portfolio.md) beschreibt die lokale
Demo mit Screenshots und synthetischen Lerninhalten. Der CI-Workflow
erzeugt Software-Stücklisten je Commit. Details stehen im
[Phase-8-Verifikationsnachweis](phase-8-verification.md).
