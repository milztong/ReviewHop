# Phase-7-Verifikationsnachweis

- Datum: 2026-11-23
- Status: Abgeschlossen

## Implementierter Scope

- ADR-0018 für einen kostenfreien lokalen und optionalen Hetzner-LGTM-Stack
- OpenTelemetry-/Micrometer-Tracing und OTLP-Metrikexport für API und Worker
- persistierter W3C-Trace-Kontext mit Fortsetzung im Worker-Consumer-Span
- automatische HTTP-Client-Spans durch den konfigurierten Spring `RestClient`
- strukturierte ECS-JSON-Logs mit Trace- und Span-Korrelation
- OpenTelemetry Collector, Prometheus, Tempo, Loki, Alloy und Grafana
- versionierte Datenquellen, Sync-Dashboard sowie technische und fachliche
  Prometheus-Alerts
- kein direkter Docker-Socket für Alloy; eingegrenzter Socket-Proxy
- Grafana nur über Loopback, keine öffentlichen Telemetrie-Ports
- Desktop- und Mobile-Chromium-Projekte sowie Axe-Prüfungen für Landingpage und
  authentifiziertes Dashboard
- explizite `aria-busy`-Zustände für Dashboard- und Sync-Aktionen

## Bisher ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Backend-Kompilierung, Paketierung und Checkstyle | `mvn --batch-mode verify` erfolgreich, 0 Verstöße |
| Vollständiger Backend-Testlauf | 23 Tests einschließlich PostgreSQL-Testcontainers und Architekturregeln, 0 Fehler |
| Frontend ESLint und TypeScript | Erfolgreich |
| Frontend-Komponententests | 9 Tests, 0 Fehler |
| Next.js-Produktionsbuild | Erfolgreich |
| Lokale und Hetzner-Compose-Konfiguration | Basis und Observability-Ergänzung in allen vier Kombinationen erfolgreich validiert |
| Observability-Container | Collector, Prometheus, Tempo, Loki, Alloy, Socket-Proxy und Grafana mit den festgelegten Images gestartet; Grafana-Health `database=ok`, Prometheus-Target `up` |
| Native Konfigurationsprüfung | Collector-Validator, Promtool einschließlich 3 Alertregeln, Loki `verify-config`, Tempo `config.verify` und Alloy `validate` erfolgreich |
| Trace-Pipeline | Synthetischer OTLP-Trace über Collector nach Tempo exportiert und über seine Trace-ID mit HTTP 200 gelesen |
| Log-Pipeline | Kurzlebige Prüfzeile über Socket-Proxy und Alloy nach Loki transportiert und mit `service_name=backend` abgefragt |
| Kontrollierter Fehlerpfad | Providerfehler, begrenzte Retries und Dead-Letter-Verhalten durch die PostgreSQL-Integrationstests abgedeckt; Trace-, Metrik- und Logtransport sowie das versionierte Sync-Dashboard separat im echten Stack nachgewiesen |
| Playwright/Axe lokal | 8 Desktop-/Mobile-Chromium-Tests, 0 Fehler; keine schweren oder kritischen Axe-Verstöße und kein horizontaler Überlauf |
| GitHub Actions | Für Commit `a1eb890` auf `main` am 2026-11-23 vom Repositoryinhaber vollständig grün bestätigt |
| Aufräumen | Isolierter Prüfstack, Testbenutzer und ausschließlich dafür erzeugte Volumes vollständig entfernt |

## Exit-Bewertung

Der kontrollierte Fehlerpfad ist auf Anwendungsebene reproduzierbar getestet.
Die reale Telemetrie-Infrastruktur hat Trace- und Logdaten Ende zu Ende
transportiert, Prometheus hat den Collector erfolgreich erfasst und das
provisionierte Dashboard verwendet dieselben fachlichen Metriken und
korrelierten Logs. Der grüne GitHub-Actions-Lauf bestätigt zusätzlich Backend,
Frontend, Browser-E2E, Container- und Security-Gates für den veröffentlichten
Phase-7-Commit.

Damit sind die Exit-Kriterien von Phase 7 erfüllt.
