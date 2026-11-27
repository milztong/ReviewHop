# Kapazitäts- und Resilienz-Runbook

Dieses Runbook verifiziert die in
[ADR-0021](../adr/0021-subject-rate-limits-and-capacity-budgets.md)
festgelegten Schutz- und Kapazitätsbudgets. Der Test erzeugt ausschließlich
lokale Testdaten und keine Cloudkosten.

## Budgets

| Vertrag | Ziel |
|---|---|
| Due-Liste | 5 Requests/Sekunde, p95 unter 500 ms |
| vollständiger manueller Review-Zyklus | 1 Zyklus/Sekunde, p95 unter 1.500 ms |
| fachliche Fehler | unter 1 % |
| verworfene k6-Iterationen | 0 |
| gleichzeitige identische Requests | 8 Antworten, genau ein persistiertes Ergebnis |
| authentifiziertes Lesen | 300 Requests pro Minute und OIDC-Identität |
| authentifiziertes Schreiben | 60 Requests pro Minute und OIDC-Identität |

Die Werte sind ein Release-Sicherheitsnetz für die kleine Ein-Server-Demo und
keine maximale Kapazitätsaussage.

## Lokaler HTTP-Test

Voraussetzungen sind Docker, eine lokale `.env` und ein laufender Stack:

```powershell
docker compose up --detach --build --wait
bash scripts/enable-performance-client.sh
docker compose -f compose.yml -f compose.performance.yml --profile performance `
  run --rm performance run --no-usage-report /scripts/critical-path.js
```

Das Hilfsskript aktiviert Client Credentials nur im lokalen, kurzlebigen
Keycloak-Zustand. Die versionierte Realm-Datei und die Produktionskonfiguration
behalten `serviceAccountsEnabled=false`. Nach dem Test den lokalen Stack
einschließlich der Testdaten entfernen:

```powershell
docker compose down --volumes --remove-orphans
```

Ein Lauf ist nur erfolgreich, wenn alle k6-Thresholds grün sind. Ergebnisse
werden mit Commit, Umgebung, CPU/RAM und Datum dokumentiert; Secrets, Tokens,
Benutzerkennungen und Payloads gehören nicht in Berichte.

## Konkurrenz- und Ausfallverträge

`ConcurrentIdempotencyIntegrationTest` beweist mit echtem PostgreSQL, dass acht
gleichzeitige Wiederholungen derselben Schreibabsicht nur einen fachlichen
Datensatz erzeugen. `RateLimitIntegrationTest` prüft getrennte Lese-/
Schreibkontingente, Header, Problem Detail, Metrik und die Ausnahme für den
Systemstatus.

Die vorhandenen Codewars- und Worker-Tests prüfen zusätzlich:

- Timeout und HTTP 5xx als retryfähige Providerfehler,
- HTTP 429 einschließlich `Retry-After`,
- Teilimport ohne vorzeitigen Cursorfortschritt,
- begrenzte Wiederholung mit Backoff/Jitter,
- Dead Letter nach fünf erfolglosen Versuchen,
- exklusives Lease und at-least-once-fähige Wiederaufnahme.

## Diagnose bei Budgetüberschreitung

1. Erst fachliche Fehler und 429-Antworten prüfen; ein künstlich zu hoher Test
   darf nicht als Latenzproblem fehlklassifiziert werden.
2. Hikari-Pool, PostgreSQL-Locks und JVM-CPU/Heap prüfen.
3. Traces der betroffenen Operation nach Datenbank- oder OIDC-Latenz aufteilen.
4. Bei Providerfehlern nicht das API-Limit erhöhen, sondern Retry-/Dead-Letter-
   Zustand und Provider-Metriken prüfen.
5. Budgets oder Limits nie allein zur Herstellung eines grünen Tests anheben.
   Eine Änderung benötigt Messergebnis, Begründung und Aktualisierung von
   ADR, Runbook und CI-Skript.

## Betrieb und Alert

Prometheus meldet wiederholte Ablehnungen über
`ReviewHopAuthenticatedRateLimitRejections`. Einzelne 429-Antworten sind
erwartbares Schutzverhalten. Wiederholte Ablehnungen erfordern die Prüfung, ob
ein fehlerhafter Client wiederholt, ein Benutzer automatisiert zugreift oder
die Budgets für einen legitimen neuen Anwendungsfall neu bewertet werden
müssen. Die Metrik enthält bewusst keine Identität oder konkrete Route.
