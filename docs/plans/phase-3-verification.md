# Phase-3-Verifikationsnachweis

- Stand: 2026-11-21
- Status: In Arbeit
- Cloudkosten ausgelöst: Nein

## Implementierter Slice

- Keycloak-Realm ohne feste Benutzerpasswörter oder committed Secrets
- aktuelle CVE-2026-114938-Mitigation für das lokale Keycloak-Image
- Auth.js Authorization Code Flow und serverseitige Tokenweitergabe im Next.js-BFF
- Spring Security OAuth2 Resource Server mit Issuer- und Audience-Prüfung
- `AppUser`-Provisionierung aus Issuer und Subject
- getrennte PostgreSQL-Rollen für Flyway und Anwendung
- manuelle `SourceItem`-/`LearningItem`-Erstellung mit einer `APPROACH_RECALL`-Karte
- Due-Liste, idempotenter Review-Abschluss und unveränderliche Review-Historie
- benutzergebundene Ansicht der letzten 20 Reviews mit Titel, Bewertung, Dauer, nächstem Termin und optionaler Notiz
- Playwright-E2E für zwei echte Keycloak-Benutzer über Login, Erstellen, Review, History und Benutzerisolation
- isolierter GitHub-Actions-E2E-Job mit dynamischen maskierten Secrets, kurzlebigen Testbenutzern und garantiertem Volume-Rückbau
- voroptimiertes Keycloak-Produktionsimage mit PostgreSQL, internem Management-Port und Ressourcenlimit
- fester öffentlicher OIDC-Issuer sowie separater, per CIDR geschützter Admin-Hostname hinter Caddy
- getrennte Keycloak-Datenbank und nichtprivilegierte Datenbankrolle
- Keycloak-Sicherheitsaudit ohne Speicherung von Admin-Event-Repräsentationen
- deterministische `phase3-simple-v1`-SchedulingPolicy hinter dem Port
- minimale responsive Oberfläche

## Ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Backend-Build und Checkstyle | Erfolgreich |
| ArchUnit und Spring Modulith | Erfolgreich |
| SchedulingPolicy-Tests | 4 erfolgreich |
| Backend-Testlauf | 11 Tests erfolgreich |
| PostgreSQL-/Security-Integrationstests | Erfolgreich mit PostgreSQL 17.12 über Testcontainers; Flyway V1 bis V4 angewendet |
| Frontend ESLint | Erfolgreich |
| Frontend TypeScript | Erfolgreich |
| Frontend Vitest | 8 Tests erfolgreich, einschließlich Review-Verlauf, serverseitiger Tokenweitergabe und Secure-Cookie-Auswahl |
| Browser-E2E | 1 Playwright-Test erfolgreich gegen den lokalen Compose-Stack mit zwei echten Keycloak-Benutzern; Alice-Daten bleiben für Bob unsichtbar |
| Remote-CI-E2E | GitHub Actions Run 44766950644 für Commit `a8d034b` erfolgreich; Secret-Erzeugung, Stackstart, Keycloak-Provisionierung, Zwei-Benutzer-Flow und Volume-Rückbau grün |
| Next.js Produktionsbuild | Erfolgreich |
| Lokaler Compose-Stack | PostgreSQL, Keycloak, Backend und Frontend healthy; BFF und Datenbank `READY` |
| Keycloak OIDC | Discovery, Auth.js-Redirect und Loginmaske erfolgreich; Callback ist `http://localhost:3000/api/auth/callback/keycloak` |
| Keycloak CVE-Mitigation | Laufende JVM meldet `UseBASE64Intrinsics=false` |
| Keycloak-Image-Scan | Vollständiger Scan ohne `--ignore-unfixed`: keine Critical-Funde; sechs High-Funde einzeln bewertet, eng befristet ausgenommen und mit `--show-suppressed` weiterhin sichtbar |
| Hetzner-Keycloak-Produktionsmodus | Isolierter Compose-Start erfolgreich; Profil `prod`, `start --optimized`, PostgreSQL, Realm-Import und Readiness auf internem Management-Port 9000 verifiziert |
| Keycloak-Container-Härtung | Read-only-Root, `cap_drop: ALL`, `no-new-privileges`, flüchtige Runtime-Pfade und 1,5-GiB-Speicherlimit effektiv verifiziert |
| Keycloak-Datenbanktrennung | Eigene Datenbank und Rolle ohne Superuser-, Create-Database- oder Create-Role-Rechte verifiziert |
| Keycloak Backup/Restore | Logischer Dump erfolgreich; Restore in getrennte Testdatenbank ergab den erwarteten Realm `ankileet` |
| Produktiver OIDC-Vertrag | Discovery liefert den festen HTTPS-Issuer; Realm enthält ausschließlich die produktive Callback-URI der Demo |
| Keycloak-Adminzugang | Echter Caddy-HTTP-Test: öffentlicher `/admin`-Pfad liefert `404`, nicht freigegebener Zugriff auf den Admin-Host `403` |
| Datenbank-Least-Privilege | Application-Rolle darf Anwendungsdaten lesen, aber weder Flyway-Historie lesen noch DDL ausführen |

## Noch offen bis zum Phase-3-Exit

- einen vom Anbieter bestätigten Keycloak-Runtime-Fix vor öffentlicher Bereitstellung einspielen; die Interim-Mitigation ist lokal wirksam verifiziert
- reale Domains, DNS, Admin-CIDRs und Secrets im geschützten GitHub-Environment konfigurieren
- freigegebenen Object-Storage-Bucket anwenden und den lokal automatisierten
  Restore gegen dieses externe Ziel nachweisen
- Guava-Risikoakzeptanz bei einer neuen Keycloak-Version oder spätestens am 15. Januar 2027 erneut bewerten
- SQLite-Risikoakzeptanzen bei einer neuen Keycloak-Version oder spätestens am 20. Dezember 2026 erneut bewerten
- Java-Trust-Store-Ausnahme bei einer neuen Keycloak-Version oder spätestens am 15. Januar 2027 erneut bewerten

Phase 3 ist damit gestartet, aber noch nicht abgeschlossen.

## Sicherheitsreview des ersten Slices

Der fokussierte zweite Review-Pass hat folgende Punkte geprüft und korrigiert:

- Der BFF unterscheidet lokale und hinter Caddy terminierte HTTPS-Requests, damit Auth.js den richtigen sicheren Cookie-Namen entschlüsselt.
- Das Access-Token wird nicht in die clientseitige Session übernommen und ausschließlich auf festen BFF-Routen als Bearer-Token weitergereicht.
- Alle Objektabfragen enthalten die interne `AppUserId`; fremde Karten liefern `404` statt Existenzinformationen.
- Gleichzeitige Wiederholungen desselben Schreibrequests werden über einen transaktionalen PostgreSQL-Advisory-Lock serialisiert; Datenbank-Constraints bleiben die zweite Schutzschicht.
- Die Application-Datenbankrolle besitzt nur explizite DML-Rechte auf Anwendungstabellen und keine Rechte auf `flyway_schema_history`.
- Manuelle Ziel-URLs akzeptieren ausschließlich absolute HTTPS-URLs ohne eingebettete Benutzerinformationen; der Server ruft diese URL nicht ab.

Der echte lokale Keycloak-/PostgreSQL-Lauf, der Zwei-Benutzer-Browser-E2E und die aktuelle Keycloak-JVM-Mitigation wurden verifiziert. Der Test meldet beide Benutzer über den realen Authorization Code Flow an und prüft den kritischen Weg bis zur benutzergebundenen History. Der CI-Job erzeugt seine Secrets pro Lauf, maskiert sie vor weiterer Verwendung und entfernt Container samt Volumes auch nach einem Fehler; [GitHub Actions Run 44766950644](https://github.com/milztong/AnkiLeet/actions/runs/44766950644) bestätigt diesen Ablauf auf einem echten Runner. Ein zunächst erkannter falscher Auth.js-Callback über `0.0.0.0` wurde durch eine explizite umgebungsabhängige `AUTH_URL` korrigiert. Der Keycloak-Scan wertet auch ungepatchte Funde aus; sechs exakte CVE-/Paketpaare sind begründet, sichtbar und zeitlich befristet ausgenommen.

Das Hetzner-Hardening ist lokal als isolierter Produktionsstack verifiziert. Der normale
OIDC-Host veröffentlicht weder Admin-Pfade noch den `master`-Realm; der separate
Admin-Host ist auf freigegebene CIDRs begrenzt. Keycloak verwendet eine getrennte
PostgreSQL-Datenbank, und deren Dump wurde erfolgreich in eine Testdatenbank restauriert.
Eine öffentliche Bereitstellung bleibt bis zur Freigabe der realen Domains, Secrets,
Admin-CIDRs, des freigegebenen und real geprüften externen Backupziels sowie des
Runtime-Fixes blockiert.
