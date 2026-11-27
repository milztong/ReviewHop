# Security Review

- Prüfstand: 2026-11-25
- Scope: Anwendung, BFF/OIDC, Provider-Sync, Datenhaltung, Observability,
  Container, CI/CD und Hetzner-Zielbetrieb
- Status: In Arbeit
- Grundlage: [Threat Model](threat-model.md)

## Ergebnisübersicht

Im geprüften Bestand wurde kein unmittelbar ausnutzbarer kritischer Fehler
festgestellt. Ein öffentlicher Portfolio-Release bleibt dennoch blockiert, bis
die unten als Release-Blocker gekennzeichneten Funktionen und Betriebsnachweise
abgeschlossen sind. Bereits akzeptierte, befristete Paketfunde bleiben in den
separaten Bewertungen sichtbar.

## Bestätigte Kontrollen

- OIDC Resource Server mit fester Issuer-, JWK- und Audience-Prüfung,
- Access Token ausschließlich im BFF und nicht in der Browser-Session,
- serverseitige Objektberechtigungen mit Zwei-Benutzer-Negativtests,
- idempotente Reviews und Imports sowie optimistisches Locking,
- Transactional Outbox mit Lease, begrenzten Retries und Dead Letter,
- getrennte PostgreSQL-Rollen für Migration und Anwendung,
- Provider-Timeouts, Eingabegrenzen und klassifizierte Fehler,
- keine serverseitigen Abrufe manuell eingetragener Fremd-URLs,
- Caddy mit HSTS, `nosniff`, Frame-Verbot und Referrer Policy,
- keine öffentlichen Ports für API, Datenbank oder Observability auf Hetzner,
- Terraform-, Container- und Paket-Scans für hohe und kritische Findings,
- strukturierte Telemetrie ohne externe Accountnamen oder Secrets.
- versionierter minimierter Datenexport und atomare idempotente Kontolöschung
  mit Zwei-Benutzer-Test und expliziter Bestätigung.
- clientseitig verschlüsseltes logisches Backup beider PostgreSQL-Datenbanken,
  dedizierte Nur-Lese-Rolle, begrenzte Retention und isolierter Restore-Test.

## Findings und Arbeitspakete

| ID | Priorität | Finding | Maßnahme / Akzeptanzkriterium | Status |
|---|---|---|---|---|
| SR-01 | Hoch | Kontolöschung und portabler JSON-Export fehlten. | PD-009 entscheiden; autorisierte, idempotente Löschung und minimierter Export mit Zwei-Benutzer-Tests implementieren. | Lokal mit PostgreSQL und Browser geprüft. CI für den endgültigen Release-Commit offen |
| SR-02 | Hoch | Externes verschlüsseltes Backupziel, Retention und realer Restore-Nachweis fehlten. | TD-014 entscheiden; App- und Keycloak-Dump isoliert wiederherstellen und Ergebnis dokumentieren. | Lokal verifiziert. Object-Storage-Apply und externer Drill bleiben Release-Blocker |
| SR-03 | Hoch | Repository-Secrets wurden bisher nicht automatisiert blockiert. | Trivy-Secret-Scan einschließlich Markdown in jedem Pull Request und Push ausführen. | In diesem Arbeitspaket behoben |
| SR-04 | Hoch | Frontend-Dependencies wurden nicht als vollständiger npm-Baum blockierend auditiert. | `npm audit --audit-level=high` reproduzierbar lokal und in CI ausführen. | In diesem Arbeitspaket behoben |
| SR-05 | Mittel | Es fehlte ein messbares Kapazitäts- und Resilienzziel. | Kritische Lese-/Schreibpfade, parallele Idempotenz und Providerausfall mit festem Budget testen. | Lokal behoben; CI-Nachweis offen |
| SR-06 | Mittel | Es fehlte eine explizite Rate-Limit-Strategie für die öffentliche Demo. | Subjektbezogene Anwendungsgrenzen und sinnvolle Antwortcodes festlegen; Login separat über Keycloak bewerten. | Lokal behoben; CI-Nachweis offen |
| SR-07 | Mittel | Eine restriktive Content Security Policy fehlte. | Next.js/Auth.js-kompatible CSP entwerfen und per Browser-/Header-Test verifizieren, ohne OIDC zu brechen. | Lokal mit Produktionsbuild und Keycloak verifiziert. HTTPS-Nachweis beim Deployment offen |
| SR-08 | Mittel | GitHub Actions waren mit veränderlichen Versions-Tags referenziert. | Externe Actions nach Herkunftsprüfung auf vollständige Commit-SHAs pinnen und Updateweg dokumentieren. | Implementiert und mit Actionlint geprüft. CI für den aktuellen Arbeitsstand offen |
| SR-09 | Mittel | Sicherheitsrelevante Fachaktionen besitzen noch keinen vollständig einheitlichen Auditvertrag. | Export und Kontolöschung sind datensparsam instrumentiert; den Operator-Retry ergänzen und gemeinsame Retention festlegen. | Teilweise behoben |
| SR-10 | Mittel | Outbox- und Telemetrie-Aufbewahrung besitzen nur kurze technische Defaults, aber keine vollständige Betriebsregel. | Retention und sichere Bereinigung mit Metrik/Test dokumentieren. | Offen |
| SR-11 | Niedrig | Ein Zugriff auf OpenAPI und Swagger UI soll für die öffentliche Demo ausgeschlossen sein. | Springdoc im Hetzner-Backend deaktivieren und öffentliche Routen nach dem Deployment prüfen. | Hetzner-Konfiguration lokal validiert. Öffentlicher HTTP-404-Nachweis offen |
| SR-12 | Niedrig | SBOM und Release-Provenienz werden nicht als Artefakte veröffentlicht. | CycloneDX/SPDX-SBOM und nachvollziehbare Image-Digests für den Release erzeugen. | CI-Inventarisierung implementiert. Upload-Nachweis und veröffentlichte Image-Digests offen |

## Browser und Workflow-Härtung

Next.js erzeugt pro Dokumentaufruf einen kryptografisch zufälligen Nonce und
setzt die CSP auf Anfrage und Antwort. Die HTML-Seiten werden pro Aufruf
gerendert. Der Produktionsheader verbietet unautorisierte Inline-Skripte und
Inline-Styles, Frames, eingebettete Objekte und eine fremde Basis-URL. Formulare
dürfen zum eigenen Ursprung und zum konfigurierten OIDC-Ursprung navigieren.
Die Entscheidung folgt dem [Next.js-CSP-Leitfaden](https://nextjs.org/docs/app/guides/content-security-policy).

Die Commit-IDs der externen Actions wurden aus den bisher verwendeten Tags
in den jeweiligen offiziellen Repositories aufgelöst. Dependabot schlägt
wöchentlich Updates für GitHub Actions vor. Beim Review eines Update-PRs
werden Herkunft, Release Notes, Berechtigungen und CI-Ergebnisse geprüft.

## Befristet akzeptierte Paketfunde

- Backend: zwei nicht erreichbare Spring-MVC-Pfade; Ablauf 2026-12-20,
- Keycloak: eng begrenzte Guava-, SQLite- und Java-Funde; Abläufe
  2026-12-20 beziehungsweise 2027-01-15,
- Keycloak-Runtime-Mitigation `-XX:-UseBASE64Intrinsics` bleibt bis zu einem
  bestätigten Herstellerfix erforderlich.

Details und genaue Paket-URLs stehen in
[Backend-Schwachstellenbewertung](backend-vulnerability-review.md) und
[Keycloak-Schwachstellenbewertung](keycloak-vulnerability-review.md).

## Releaseentscheidung

Der Security Review ist erst abgeschlossen, wenn:

1. kein unbehandeltes kritisches oder hohes Finding offen ist,
2. SR-01 im Release-CI bestätigt und SR-02 nachweisbar erfüllt ist,
3. Last-/Resilienzbudget und Rate-Limit-Entscheidung dokumentiert sind,
4. befristete Ausnahmen am Ablaufdatum erneut geprüft wurden,
5. CI, Container-Scans, Secret-Scan, Dependency-Audit und Browser-E2E für den
   Release-Commit grün sind.
