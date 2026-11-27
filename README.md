# ReviewHop

ReviewHop ist eine Lernanwendung, die bereits bearbeitete Aufgaben aus Lernplattformen in planbare Wiederholungen überführt. Der fachliche Kern verbindet externe Lerninhalte mit unterschiedlichen Wiederholungsmodi und einem Spaced-Repetition-Scheduler.

Das Projekt wurde als AnkiLeet entwickelt. Bestehende lokale Anmeldungen
und Datenvolumes behalten ihre technischen Kennungen.
[Kompatibilität beim Namenswechsel](docs/adr/0022-reviewhop-brand-and-compatible-identifiers.md)

**Phase 8 – Hardening und Portfolio-Release** ist in Arbeit. Phase 7 ist
abgeschlossen: Der asynchrone Codewars-Sync propagiert seinen Trace-Kontext von
der API über die PostgreSQL-Outbox zum Worker und Provider. Ein kostenfrei
zuschaltbarer lokaler Stack aus OpenTelemetry Collector, Prometheus, Tempo,
Loki, Alloy und Grafana liefert Dashboard und Alerts. Phase 8 ergänzt Threat
Model, Security Review, Privacy-Lebenszyklus, verschlüsselte Backups sowie
subjektbezogene API-Limits und messbare Last-/Resilienzbudgets.

Der Portfolio-Rundgang zeigt das lokale Setup, den Review-Ablauf mit
synthetischen Beispielen, Screenshots und den Stand der öffentlichen Demo.
[Portfolio und Demo](docs/demo/portfolio.md)

## Dokumentation

- [Produktvision](docs/product/vision.md)
- [Glossar](docs/product/glossary.md)
- [Offene Entscheidungen](docs/product/open-decisions.md)
- [Roadmap](docs/plans/roadmap.md)
- [Definition of Done](docs/plans/definition-of-done.md)
- [Phase-1-Verifikationsnachweis](docs/plans/phase-1-verification.md)
- [Phase-2-Verifikationsnachweis](docs/plans/phase-2-verification.md)
- [Phase-3-Verifikationsnachweis](docs/plans/phase-3-verification.md)
- [Phase-4-Verifikationsnachweis](docs/plans/phase-4-verification.md)
- [Phase-5-Verifikationsnachweis](docs/plans/phase-5-verification.md)
- [Phase-6-Verifikationsnachweis](docs/plans/phase-6-verification.md)
- [Backup- und Restore-Runbook](docs/operations/backup-restore-runbook.md)
- [Kapazitäts- und Resilienz-Runbook](docs/operations/capacity-resilience-runbook.md)
- [Phase-7-Verifikationsnachweis](docs/plans/phase-7-verification.md)
- [Phase-8-Verifikationsnachweis](docs/plans/phase-8-verification.md)
- [Phase-8-Produktentscheidungen](docs/product/phase-8-decisions.md)
- [Threat Model](docs/security/threat-model.md)
- [Security Review](docs/security/security-review.md)
- [Observability-Runbook](docs/operations/observability-runbook.md)
- [Keycloak-Schwachstellenbewertung](docs/security/keycloak-vulnerability-review.md)
- [Hetzner-Demo-Runbook](docs/operations/hetzner-demo-runbook.md)
- [Cloud-Kostenleitplanken](docs/operations/cloud-cost-guardrails.md)
- [Terraform-Struktur](infrastructure/README.md)
- [C4-Systemkontext](docs/architecture/system-context.md)
- [C4-Containerdiagramm](docs/architecture/container-diagram.md)
- [C4-Komponentenmodell](docs/architecture/backend-components.md)
- [Domänenmodell](docs/architecture/domain-model.md)
- [Architecture Decision Records](docs/adr/README.md)

## Vorgesehener Stack

- Java 21 und Spring Boot 3
- Spring Data JPA/Hibernate, Spring Security und OAuth2/OIDC
- PostgreSQL und Flyway
- Next.js und TypeScript
- OpenAPI
- JUnit 5, Mockito, Testcontainers, WireMock, ArchUnit und Playwright
- Hetzner Cloud, Docker Compose und Caddy; AWS optional als Referenz-Deployment
- Terraform und GitHub Actions mit geschütztem SSH-Deployment
- OpenTelemetry, Prometheus, Grafana, Loki und Tempo

## Lokal starten

Voraussetzungen:

- Docker mit Docker Compose
- alternativ für Einzelstarts: Java 21, Maven 3.9+, Node.js 24 und npm 10+

Den vollständigen Stack starten:

```powershell
docker compose up --build
```

Docker Compose startet API und Sync-Worker aus demselben Backend-Build als
getrennte Prozesse. Nur die API besitzt einen HTTP-Port; der Worker greift über
die interne PostgreSQL-Outbox auf fällige Aufträge zu.

Den kostenfreien lokalen Observability-Stack bewusst zuschalten:

```powershell
docker compose -f compose.yml -f compose.observability.yml up --build --wait
```

Danach sind erreichbar:

- Web-Anwendung: <http://localhost:3000>
- Java-API: <http://localhost:8080/api/v1/system/status>
- Readiness: <http://localhost:8080/actuator/health/readiness>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Keycloak: <http://localhost:8180>
- Grafana bei aktivierter Observability-Ergänzung: <http://127.0.0.1:3001>

Vor dem ersten Start wird `.env.example` nach `.env` kopiert und jeder Secret-Platzhalter durch einen eigenen zufälligen Wert ersetzt. Der lokale Keycloak-Realm erlaubt danach die Registrierung eines Entwicklungsbenutzers. Ein vorhandenes Phase-1-/Phase-2-PostgreSQL-Volume muss wegen der neuen getrennten Datenbankrollen einmal lokal neu erstellt werden.

Stack und Datenvolume entfernen:

```powershell
docker compose down --volumes
```

Das Löschen des Volumes entfernt alle lokalen PostgreSQL-Daten.

## Checks ausführen

Backend inklusive Architektur- und PostgreSQL-Testcontainers-Tests:

```powershell
Set-Location backend
mvn --batch-mode --no-transfer-progress verify
```

Frontend:

```powershell
Set-Location frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

Die PostgreSQL-Integrationstests benötigen einen laufenden Docker-Daemon.

Der lokale Browser-E2E-Test benötigt den laufenden Compose-Stack und zwei aktivierte
Keycloak-Benutzer mit vollständigem Profil und ohne offene Required Actions. Zugangsdaten
werden ausschließlich als Prozessvariablen übergeben und nicht in Dateien gespeichert:

```powershell
Set-Location frontend
npx playwright install chromium
$env:E2E_ALICE_USERNAME = "alice"
$env:E2E_ALICE_PASSWORD = [Net.NetworkCredential]::new(
  "", (Read-Host "Alice password" -AsSecureString)).Password
$env:E2E_BOB_USERNAME = "bob"
$env:E2E_BOB_PASSWORD = [Net.NetworkCredential]::new(
  "", (Read-Host "Bob password" -AsSecureString)).Password
npm run test:e2e
```

In GitHub Actions übernimmt der Job `Browser E2E` die vollständige Provisionierung.
Alle Datenbank-, OIDC- und Benutzerkennwörter werden pro Lauf zufällig erzeugt, vor der
weiteren Verwendung maskiert und nicht als Repository-Secret benötigt. Container und
Volumes werden auf dem kurzlebigen Runner auch nach einem fehlgeschlagenen Test entfernt.
