# Phase-1-Verifikationsnachweis

Stand: 20. November 2026

## Ergebnis

Der lokale Walking Skeleton ist implementiert und ausführbar. Ein Browser erreicht das Next.js-Frontend, dessen serverseitige BFF-Route die Spring-Boot-API aufruft. Die API prüft ihre PostgreSQL-Verbindung über einen Ausgangsport und liefert den Zustand über einen versionierten, in OpenAPI dokumentierten Endpunkt aus.

Der formale Phasenabschluss ist durch den erfolgreichen GitHub-Actions-Lauf für Commit `a8d034b` belegt. Backend, Frontend, Terraform, Container-Build und die inzwischen ergänzten Phase-3-Browser-Tests liefen auf GitHub-Runnern erfolgreich.

## Implementierter Pfad

```text
Browser
  -> Next.js-Seite
  -> GET /api/system-status (BFF)
  -> GET /api/v1/system/status (Spring Boot)
  -> DatabaseProbe-Port
  -> Spring Data JPA
  -> PostgreSQL 17.12
```

Die BFF-Route begrenzt den Backend-Aufruf zeitlich, validiert dessen Antwort und gibt bei einem nicht erreichbaren oder vertragswidrigen Backend eine bereinigte `503`-Antwort zurück. Der öffentliche Backend-Endpunkt antwortet ebenfalls mit `503`, wenn die Datenbankprüfung fehlschlägt.

## Ausgeführte Prüfungen

| Bereich | Befehl oder Prüfung | Ergebnis |
|---|---|---|
| Backend | `mvn --quiet --batch-mode --no-transfer-progress verify` | Erfolgreich: 6 Tests, Spring-Modulith-/ArchUnit-Regeln, PostgreSQL-Testcontainers und Checkstyle |
| Frontend | `npm run lint` | Erfolgreich, keine Warnungen |
| Frontend | `npm run typecheck` | Erfolgreich |
| Frontend | `npm run test` | Erfolgreich: 5 Tests in 2 Testdateien |
| Frontend | `npm run build` | Erfolgreicher Next.js-Produktionsbuild |
| Container | `docker compose build` | Backend- und Frontend-Image erfolgreich gebaut |
| Laufzeit | `docker compose up --detach` und Healthchecks | PostgreSQL, Backend und Frontend gesund |
| API | `/api/v1/system/status` | `200`, Anwendung `ankileet-api`, Datenbank `READY` |
| BFF | `/api/system-status` | `200`, Zustand `ready`, Datenbank `READY` |
| Betrieb | `/actuator/health/readiness` | `UP` |
| Vertrag | `/v3/api-docs` | OpenAPI-Dokument vorhanden; Systemstatus-Pfad enthalten |
| Browser | Desktop- und 390-x-844-Ansicht | Responsive, kein horizontaler Überlauf, Status sichtbar, keine Konsolenfehler |
| Abhängigkeiten | `npm audit` | Keine bekannten npm-Schwachstellen im geprüften Lockfile |

## Automatisierte CI-Gates

Der Workflow `.github/workflows/ci.yml` führt bei Pull Requests und Pushes nach `main` aus:

1. Backend-`verify` einschließlich Architektur- und Integrationstests,
2. Frontend-Installation per `npm ci`, Lint, Typprüfung, Tests und Build,
3. Compose-Konfigurationsprüfung und den Bau beider Images.

Der Workflow verwendet nur Leserechte auf Repository-Inhalte und bricht ältere Läufe desselben Branches bei einem neueren Lauf ab.

Der erste echte Lauf auf `main` wurde am 20. November 2026 erfolgreich abgeschlossen: [GitHub Actions Run 44766950644](https://github.com/milztong/AnkiLeet/actions/runs/44766950644).

## Bewusst nicht Bestandteil von Phase 1

- Authentifizierung, Autorisierung und benutzereigene Daten beginnen in Phase 3.
- Cloud-Ressourcen und Terraform beginnen in Phase 2. ADR-0012 legt inzwischen Hetzner als primäre Demo und AWS als optionale Referenz fest; jede kostenpflichtige Provisionierung benötigt Budgetentscheidung und ausdrückliche Freigabe.
- Fachliche Schreibpfade, Idempotenz und Nebenläufigkeit existieren noch nicht.
- Provider-Integrationen, Queueing und Observability über die Basis-Healthchecks hinaus folgen in späteren Phasen.

## Verbleibende Hinweise

- Spring Boot 3.5 ist die letzte Spring-Boot-3-Nebenlinie. Ein späterer Upgrade-Pfad wird vor einem öffentlichen Release bewertet, ohne den für das Portfolio gewünschten Spring-Boot-3-Nachweis aus Phase 1 rückwirkend umzubauen.
- Der lokale Compose-Stack läuft nach Abschluss der Prüfung weiter und ist unter `http://localhost:3000` erreichbar.
- Commit `a8d034b` wurde auf `origin/main` veröffentlicht; es wurde keine Cloud-Ressource angelegt.
