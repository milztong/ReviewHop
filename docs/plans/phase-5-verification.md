# Phase-5-Verifikationsnachweis

- Datum: 2026-11-22
- Status: Abgeschlossen

## Gelieferter Scope

- capability-basierter `ProviderConnector` und authentifizierter Provider-Katalog
- öffentliche, ausdrücklich `UNVERIFIED` markierte Codewars-Profilzuordnung
- fester offizieller Codewars-API-v1-Adapter ohne Credentials oder
  benutzergesteuerte Ziel-URL
- vollständige nullbasierte Pagination mit High-Water-Mark-Cursor
- synchrone, manuell gestartete und benutzergebundene SyncJobs
- idempotenter Learning-Import über die veröffentlichte `learning::api`
- additive Flyway-Migration V6 für ProviderConnection und SyncJob
- Retry-Klassifikation für Timeout, Netzwerk, `429` und `5xx`
- sichtbare terminale und retryable Fehler inklusive `Retry-After`
- genau ein laufender Sync pro Verbindung sowie Recovery eines nach 30 Minuten
  veralteten RUNNING-Jobs
- strukturierte Sync-Logs ohne Benutzernamen sowie Provider-Request-Counter und
  Latenz-Timer mit festen Tags
- Next.js-BFF-Routen und capability-gesteuerte Codewars-Karte im Dashboard

## Ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| WireMock-Providervertrag | 4 Tests erfolgreich: Profil-/Seitenerfolg, Pagination-Metadaten, Timeout, `429` mit `Retry-After`, `5xx` und ungültige Antworten |
| PostgreSQL-/Security-Integration | 3 Tests erfolgreich mit PostgreSQL 17.12: zwei Seiten, Wiederholungsimport, Idempotency-Key-Konflikt, fremder Benutzer, Teilfehler ohne Cursorfortschritt und Stale-Sync-Recovery |
| ArchUnit und Spring Modulith | 2 Tests erfolgreich; `integration` darf nur `identity::api` und `learning::api` verwenden, Domain bleibt frameworkfrei |
| Gesamter Backend-Testlauf | `mvn verify`: 21 Tests, 0 Fehler, 0 übersprungen |
| Maven Verify und Checkstyle | Erfolgreich; 0 Checkstyle-Verstöße |
| Frontend-Komponententests | 9 Tests in 4 Dateien erfolgreich, einschließlich Profilverbindung und Sync-Rückmeldung |
| Frontend TypeScript und ESLint | `npm run typecheck` und `npm run lint` erfolgreich |
| Next.js-Produktionsbuild | Erfolgreich; Provider-, Connection- und Sync-BFF-Routen im Route-Manifest |
| Container-Build | Finale Backend- und Frontend-Produktionsimages erfolgreich gebaut; finales Backend-Image nach dem Nebenläufigkeitsreview erneut gebaut |
| Diff-Prüfung | `git diff --check` ohne Whitespace-Fehler; keine offenen TODO/FIXME im neuen Anwendungscode |

## Abgleich mit der Definition of Done

- Externe HTTP-Aufrufe liegen außerhalb von Datenbanktransaktionen; Seitenimporte
  und Jobübergänge verwenden kurze Transaktionen.
- `(provider, externalId)` und `(userId, sourceItemId)` schützen die fachliche
  Idempotenz zusätzlich zu benutzerbezogenen Request-Idempotency-Keys.
- Bereits vorhandene LearningItems, Notizen, Review Cards und ReviewLogs werden
  beim Wiederholungsimport nicht verändert.
- Jeder Objektzugriff auf ProviderConnection und SyncJob wird serverseitig an den
  aktuellen AppUser gebunden; ein fremder Benutzer erhält `404`.
- REST-Endpunkte sind unter `/api/v1` versioniert, mit OpenAPI annotiert und liefern
  bei Provider- und Konfliktfehlern Problem Details.
- V6 ist additiv und wurde auf einem leeren realen PostgreSQL-Schema ausgeführt.
- Codewars-Ziel- und Kata-URLs stammen aus festen HTTPS-Basisadressen; der Benutzer
  kann keine Zieladresse für Serverrequests vorgeben.
- Passwörter, Tokens, Session-Cookies, Aufgabenbeschreibungen und Lösungen werden
  weder angefordert noch gespeichert. Benutzerspezifische `completedLanguages`
  werden nicht in das globale SourceItem fehlmodelliert.
- WireMock ist ausschließlich Testabhängigkeit; es wurde keine neue
  Laufzeitbibliothek für den Providerzugriff eingeführt.
- Cloud/Terraform ist für dieses Arbeitspaket nicht betroffen; es wurden keine
  Cloud-Ressourcen oder kostenpflichtigen Dienste angelegt.

## CI-Bestätigung

- Der erweiterte Playwright-Flow für die unverifizierte Codewars-Verbindung wurde
  mit den in CI ephemer erzeugten OIDC-/E2E-Secrets ausgeführt.
- GitHub Actions hat Backend, Frontend, Browser-E2E, Containerbau und die
  bestehenden Trivy-Gates für Commit `6af04c5` vollständig grün bestätigt. Die
  Bestätigung wurde am 2026-11-22 durch den Repository-Eigentümer gemeldet.

## Verbleibende Risiken

- Codewars bezeichnet API v1 selbst als minimal und inkonsistent und dokumentiert
  keine Sortiergarantie. Phase 5 liest deshalb für Korrektheit alle gemeldeten
  Seiten; große Profile benötigen entsprechend mehr API-Aufrufe.
- Der Sync blockierte in Phase 5 den Benutzerrequest. Phase 6 hat dieses Risiko
  inzwischen durch Transactional Outbox, getrennten Worker und begrenzte
  automatische Retries mit Backoff/Jitter behoben.
- Codewars liefert in der Completed-Liste keine Schwierigkeit oder globalen
  Challenge-Tags. Zusätzliche Einzelabfragen wurden bewusst vermieden.
- Die frühere 30-Minuten-Stale-Erkennung wurde in Phase 6 durch exklusives Leasing,
  Lease-Erneuerung pro Seite und at-least-once-Redelivery ersetzt.

## Verbindliche Entscheidungen

- [ADR-0003](../adr/0003-provider-connector-strategy.md)
- [ADR-0016](../adr/0016-codewars-sync-consistency.md)
- [Phase-5-Produktentscheidungen](../product/phase-5-decisions.md)
