# Phase-8-Verifikationsnachweis

- Startdatum: 2026-11-24
- Status: In Arbeit

## Ziel

ReviewHop wird als sicher reproduzierbare, betreibbare und verständliche
Portfolio-Demo releasefähig. Phase 8 wird in kleine, unabhängig prüfbare
Arbeitspakete geteilt.

## Arbeitspakete

| Paket | Ergebnis | Status |
|---|---|---|
| 8.1 Security-Baseline | Threat Model, Security Review, Secret-Scan und npm Audit | Baseline implementiert. Abschließender Security Review offen |
| 8.2 Privacy-Lebenszyklus | Produktentscheidung, Kontolöschung und JSON-Export | Lokal verifiziert |
| 8.3 Betriebsresilienz | Backup/Restore, Retention und Wiederanlauf | Lokal verifiziert. Externer Restore-Drill offen |
| 8.4 Last und Resilienz | Kapazitätsbudget, Konkurrenz- und Ausfalltests | Lokal verifiziert |
| 8.5 Portfolio-Release | Onboarding, Demo-Daten, Screenshots, SBOM und Projektbeschreibung | Portfolio-Artefakte lokal verifiziert. Öffentliche Freigabe offen |

## CI-Status des Phase-8-Pushs

Der Repository-Inhaber meldet den GitHub-Actions-Lauf zum Phase-8-Push
als grün. Der zugehörige Commit `f4ef4e3` ist lokal auf `main`
und `origin/main` vorhanden. Die GitHub-API lieferte ohne Anmeldung
keinen Zugriff auf Laufdetails und Artefaktliste. Der gemeldete CI-Status
gilt für diesen Stand. Lauf-ID und Upload-Nachweis bleiben offen.

## Arbeitspaket 8.1 – Akzeptanzkriterien

- Assets, Vertrauensgrenzen, Missbrauchsfälle und STRIDE-Bedrohungen sind
  dokumentiert.
- Bestehende Kontrollen und Restrisiken sind konkreten Folgearbeiten zugeordnet.
- Kein erkanntes kritisches oder hohes Finding wird unbemerkt akzeptiert.
- Jeder Pull Request und Push scannt das Repository blockierend auf hohe und
  kritische Secrets.
- Der vollständige npm-Abhängigkeitsbaum wird blockierend auf hohe und kritische
  bekannte Schwachstellen geprüft.
- Die Gates sind lokal reproduzierbar und im nächsten GitHub-Actions-Lauf grün.

## Bisheriger Nachweis

| Prüfung | Ergebnis |
|---|---|
| Architektur- und Security-Bestandsaufnahme | OIDC/BFF, Ownership, Outbox, Datenbankrollen, Providergrenze, Observability, CI/CD und Hetzner-Topologie geprüft |
| Threat Model | Initiale datenflussorientierte STRIDE-Baseline erstellt |
| Security Review | Zwölf Findings priorisiert; SR-01 lokal geschlossen, SR-02 bleibt Release-Blocker |
| Repository-Secret-Scan | Trivy `0.76.2` einschließlich Markdown: alle Quell-, Konfigurations- und Dokumentationsbereiche sowie Repository-Wurzeldateien ohne Fund; generierte Verzeichnisse werden ausgeschlossen |
| Frontend-Dependency-Audit | `npm run audit:ci`: 0 bekannte Schwachstellen |
| Frontend-Regression | ESLint, TypeScript-Prüfung, 10 Vitest-Tests und Produktionsbuild erfolgreich |
| Backend-Regression | `mvn -B verify`: 24 Tests, 0 Fehler; ArchUnit, Spring Modulith, Testcontainers, Flyway und Checkstyle erfolgreich |
| Workflow-Validierung | Actionlint `1.7.12` ohne Befund |
| Dokumentation | Interne Markdown-Links und `git diff --check` ohne Befund |

Der lokale Windows-Docker-Mount wurde wegen vorhandener generierter
Verzeichnisse quellbereichsweise geprüft. Der CI-Job läuft auf einem sauberen
Checkout über das gesamte Arbeitsverzeichnis.

Der Abschluss von 8.1 schließt Phase 8 nicht ab. Die weiteren Pakete bleiben
gemäß ihrem jeweiligen Status eigenständige Release-Voraussetzungen.

## Arbeitspaket 8.2 – Privacy-Lebenszyklus

PD-009 ist in den
[Phase-8-Produktentscheidungen](../product/phase-8-decisions.md) entschieden.
[ADR-0019](../adr/0019-atomic-privacy-lifecycle.md) dokumentiert die atomare,
modulübergreifende Transaktionsgrenze.

Implementiert sind:

- versionierter JSON-Export von Profilpräferenzen, Provider-Verbindungen,
  Learning Items, Review Cards, Review Logs und SyncJobs,
- Ausschluss von OIDC-Kennungen, Idempotency Keys, Outbox-Payloads,
  Trace-Kontext und technischen Lease-Daten,
- authentifizierte, explizit bestätigte und idempotente Kontolöschung,
- atomare Löschung von `AppUser`, Provider-/Sync-/Outbox- sowie vollständigen
  Learning-/Review-Daten,
- referenzabhängige Bereinigung verwaister Source Items,
- strukturierte Audit-Logs und Metriken ohne Benutzerkennung oder Payload,
- BFF-Routen, Download und bestätigungspflichtige Oberfläche mit anschließendem
  Sign-out.

Der PostgreSQL-Integrationstest beweist Authentifizierung, Zwei-Benutzer-
Isolation, Exportminimierung, Löschung des vollständigen Datenbestands und einen
erfolgreichen wiederholten Löschaufruf. Der Browser-E2E-Vertrag für Download,
Bestätigung und Sign-out wurde lokal im Rahmen von 8.5 geprüft.

## Arbeitspaket 8.3 – Betriebsresilienz

[ADR-0020](../adr/0020-encrypted-postgresql-backups-with-restic.md) entscheidet
TD-014: Restic sichert getrennte ReviewHop- und Keycloak-Dumps clientseitig
verschlüsselt in privaten Hetzner Object Storage außerhalb der Serverregion.
Der kostenpflichtige Bucket ist in Terraform standardmäßig deaktiviert und
wurde nicht erzeugt.

Implementiert sind:

- dediziertes Backup-Image mit PostgreSQL 17.12 Client und Restic 0.20.0,
- Datenbankzugriff über eine eigene Nur-Lese-Rolle statt Anwendung oder
  Flyway-Migrator,
- Klartext-Dumps ausschließlich im flüchtigen Container-Dateisystem,
- gemeinsamer verschlüsselter Snapshot mit SHA-256-Manifest,
- tägliche Aufbewahrung von 7 Tages-, 4 Wochen- und 3 Monatsständen,
- Integritätscheck und begrenzte Retention nach jedem erfolgreichen Lauf,
- täglicher systemd-Timer mit Nachholen verpasster Läufe,
- ausschließlich isolierter Restore mit expliziter Wegwerf-Bestätigung,
- RPO 24 Stunden und RTO 60 Minuten sowie Wiederanlauf-Runbook,
- optionaler privater Object-Storage-Bucket mit Terraform-`prevent_destroy`.

Der lokale Container-Nachweis startet PostgreSQL mit ReviewHop- und
Keycloak-Testdaten, führt einen Datenbankneustart aus, sichert beide Datenbanken
mit der Nur-Lese-Rolle verschlüsselt, restauriert sie in eine zweite flüchtige
PostgreSQL-Instanz und prüft Checksummen, fünf zentrale Anwendungstabellen,
Realm `ankileet` und dessen Client. Ergebnis am 2026-11-24:
`Backup, restart and isolated restore verification passed.`

Am 26. November 2026 brach der Backup-Job in einem Dependabot-Lauf nach
erfolgreicher Backup- und Integritätsprüfung vor dem Restore mit Exit-Code 2
ab. Der CI-Test wartet jetzt vor und nach dem Neustart sowie vor dem Restore
auf PostgreSQL über TCP. Dadurch kann der temporäre Initialisierungsserver
die Prüfung nicht vorzeitig auslösen. Bei einem Timeout erscheinen die
letzten Logs des betroffenen Testcontainers. Der vollständige lokale
Backup-/Restore-Test und ShellCheck waren nach der Korrektur erfolgreich.
Die Bestätigung auf GitHub steht aus.

Das gehärtete Backup-Image wurde mit Trivy `0.76.2` gegen die aktuelle
Vulnerability-Datenbank geprüft: 0 hohe oder kritische Betriebssystemfunde.
Ein zunächst erkanntes, bereits behobenes `libxml2`-Finding wurde durch das
gezielte Upgrade auf Alpine-Paket `2.13.9-r3` geschlossen und anschließend
erneut ohne Befund gescannt. Compose, ShellCheck `0.11.0`, Actionlint `1.7.12`,
Terraform-Validierung und drei gemockte Terraform-Tests sind erfolgreich; der
dritte weist die Ablehnung eines Backup-Buckets in der Serverregion nach.

Nach expliziter Kosten- und Apply-Freigabe folgen Bucket-Erzeugung, Timer-Aktivierung auf dem realen
Server und ein dokumentierter Restore-Drill gegen das externe Repository.
Damit ist SR-02 lokal behoben, bleibt für den öffentlichen Release aber ein
Betriebsblocker.

## Arbeitspaket 8.4 – Last und Resilienz

[ADR-0021](../adr/0021-subject-rate-limits-and-capacity-budgets.md) legt die
messbaren Budgets und die Rate-Limit-Strategie für die Ein-Server-Demo fest.
Implementiert sind:

- subject- und issuerbezogene getrennte Kontingente von 300 Reads und 60 Writes
  pro Minute,
- größenbegrenzte, selbst verfallende In-Process-Zähler ohne Benutzerkennung in
  Metriklabels,
- HTTP 429 als Problem Detail mit `Retry-After` und Rate-Limit-Headern,
- technische Erfolgs-/Ablehnungsmetrik und Prometheus-Alert,
- PostgreSQL-Test mit acht gleichzeitigen identischen Learning-Item- und
  Review-Completion-Requests,
- k6-Test über reales Keycloak Client-Credentials, HTTP und PostgreSQL mit
  5 Due-Reads/Sekunde und 1 vollständigem Review-Zyklus/Sekunde,
- p95-Budgets von 500 ms für Due-Reads und 1.500 ms für den Review-Zyklus,
- weniger als 1 % fachliche Fehler und keine verworfenen Iterationen,
- bestehende WireMock-/Worker-Verträge für Timeout, HTTP 429/5xx, Backoff,
  Teilfortschritt, fünf Versuche und Dead Letter.

Das vollständige Backend-Verify mit 26 Tests einschließlich Architekturregeln,
PostgreSQL-Konkurrenztest und Rate-Limit-Integrationstest ist lokal grün. Der
k6-Lauf vom 24. November 2026 gegen Basis-Commit `a1eb89054d7f`
zuzüglich des uncommitteten Arbeitspakets lief auf Docker Desktop 29.2.1 mit
6 CPUs und 7,7 GiB RAM. Alle 213 Checks in 91 Iterationen waren erfolgreich;
es gab keine fachlichen Fehler, fehlgeschlagenen HTTP-Requests oder
verworfenen Iterationen. Die gemessene p95-Latenz betrug 15 ms für die
Due-Liste und 93,19 ms für den vollständigen Review-Zyklus. Damit liegen beide
Werte deutlich unter den Budgets von 500 beziehungsweise 1.500 ms. Der
unveränderte Realm-Vertrag lässt Service Accounts in Produktion aus; nur das
kurzlebige Performance-Hilfsskript aktiviert sie im lokalen CI-Realm.

## Arbeitspaket 8.5 – Portfolio-Release

- [Portfolio-Rundgang](../demo/portfolio.md) mit lokalem Onboarding, zwei
  synthetischen Learning Items, Screenshots und Lebenslaufbeschreibung.
- Playwright erfasst die fälligen Review Cards und den Eintrag in der
  Review-Historie nach einer tatsächlichen Bewertung im lokalen Stack.
- Der CI-Workflow archiviert die Aufnahmen sowie CycloneDX-Inventare der
  vier gebauten Images je Commit. API und Worker teilen ihr Image.

### Lokaler Nachweis vom 25. November 2026

- Der isolierte Compose-Stack wurde aus dem Arbeitsstand von Phase 8 gebaut.
  Zehn Browserprüfungen waren erfolgreich. Der separate Portfolio-Test
  prüfte Anmeldung mit einem temporär provisionierten Keycloak-Konto,
  Erstellung, Fälligkeit, Bewertung und Historie. Beide Aufnahmen wurden
  visuell geprüft. Die temporären Container und Datenvolumes wurden entfernt.
- Die vier Trivy-Inventare sind lesbare CycloneDX-1.7-Dateien mit jeweils
  201 (Backend), 194 (Frontend), 560 (Keycloak) und 141 (Backup) Komponenten.
- ESLint, TypeScript, zehn Vitest-Tests, Produktionsbuild, Actionlint,
  ShellCheck und die Prüfung der lokalen Dokumentlinks waren erfolgreich.

### Öffentliche Veröffentlichung

Vor der Veröffentlichung stehen der CI-Nachweis für den endgültigen
Release-Commit inklusive Artefakt-Upload, der Abschluss des
[Security Reviews](../security/security-review.md) und die Zuordnung der
veröffentlichten Image-Digests aus. Für die
Live-Demo fehlen der geprüfte Hetzner-Plan mit Apply-Freigabe, die Domain mit
bestätigtem HTTPS, die Betriebsfreigabe und der externe Restore-Drill.
Die [Release-Definition-of-Done](definition-of-done.md#portfolio-release)
bleibt bis zu diesen Nachweisen offen.

### Härtung nach dem Phase-8-Push

- Der Produktionsbuild erzwingt eine Content Security Policy mit frischem
  Nonce. Zehn Chromium-Tests auf Desktop und Mobil sowie der separate
  Portfolio-Test bestanden im isolierten Compose-Stack mit echtem Keycloak.
- Alle externen Actions sind auf vollständige Commit-IDs festgelegt.
  Actionlint und die Auswertung der Hetzner-Compose-Konfiguration waren
  erfolgreich. OpenAPI und Swagger UI sind im Hetzner-Backend deaktiviert.
- Der öffentliche Deployment-Workflow prüft HTTPS, HSTS, CSP,
  OIDC-Discovery und HTTP 404 für die API-Dokumentation. Der Nachweis
  auf den öffentlichen Domains steht aus.
- Der [Budgetentwurf](../operations/hetzner-demo-runbook.md#budgetentwurf-vom-25-november-2026)
  erfasst den Standardserver mit IPv4, Server-Backups und externem
  Object Storage. Die geschätzten Grundkosten von rund 14,37 € brutto
  pro Monat sind freigegeben. Die Live-Preiskontrolle steht aus.

### Namenswechsel zu ReviewHop

[ADR-0022](../adr/0022-reviewhop-brand-and-compatible-identifiers.md) dokumentiert
den neuen Produktnamen und die weiterhin gültigen technischen Kennungen.
Der Namenswechsel umfasst Oberfläche, API-Dokumentation, Java- und npm-Builds,
Container, Software-Inventare und Portfolio-Dokumentation. Der Vergleich mit
dem vorherigen Commit bestätigt unveränderte Flyway-Migrationen, den stabilen
Compose-Projektnamen und unveränderte Keycloak-IDs.

Lokal bestanden 26 Backend- und 10 Frontend-Tests, zehn reguläre
Browsertests und der Portfolio-Test. Der k6-Lauf bestand 217 Checks ohne
Fehler. Actionlint, ShellCheck, die Hetzner-Compose- und Prometheus-Prüfungen
sowie vier CycloneDX-Inventare für die neuen Image-Namen wurden verifiziert.
