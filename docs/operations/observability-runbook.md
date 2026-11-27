# Observability-Runbook

## Zweck und Grenzen

Der Phase-7-Stack verbindet strukturierte Logs, Metriken und Traces für den
kritischen Weg API → PostgreSQL-Outbox → Worker → Codewars. Er läuft lokal
kostenfrei. Die Hetzner-Variante wird nur nach einer separaten Ressourcen- und
Kostenfreigabe aktiviert. Grafana ist nie öffentlich erreichbar.

## Lokal starten

Voraussetzungen sind ein laufender Docker-Daemon, eine ausgefüllte `.env` und
ein eigenes zufälliges `GRAFANA_ADMIN_PASSWORD`.

```powershell
docker compose -f compose.yml -f compose.observability.yml up --build --wait
```

Danach ist Grafana ausschließlich lokal unter <http://127.0.0.1:3001>
erreichbar. Das provisionierte Dashboard liegt im Ordner `ReviewHop` unter
`ReviewHop Sync Overview`. Prometheus, Loki, Tempo und der OpenTelemetry
Collector besitzen keine veröffentlichten Host-Ports.

Zum Beenden ohne Datenverlust:

```powershell
docker compose -f compose.yml -f compose.observability.yml down
```

Zum bewussten Löschen aller lokalen Anwendungs- und Observability-Daten:

```powershell
docker compose -f compose.yml -f compose.observability.yml down --volumes
```

Dieser letzte Befehl ist destruktiv und entfernt auch PostgreSQL- und
Keycloak-Daten.

## Kontrollierter Fehlernachweis

Der Fehlernachweis verwendet keinen Test-Endpunkt im Produkt. Stattdessen wird
nur der Worker vorübergehend mit einem nicht erreichbaren Providerziel
gestartet:

1. Einen gültigen Codewars-Account verbinden.
2. Den normalen Worker stoppen.
3. Einen kurzlebigen Diagnose-Worker mit einem absichtlich nicht erreichbaren
   Ziel starten:

   ```powershell
   docker compose -f compose.yml -f compose.observability.yml run --rm `
     -e ANKILEET_CODEWARS_API_BASE=http://127.0.0.1:1 worker
   ```
4. Über die Oberfläche einen Sync anfordern und mindestens einen Versuch
   abwarten.
5. In Grafana den Zeitraum auf die letzten 15 Minuten setzen.

Der Trace `provider.sync.process` muss den aus dem API-Request übernommenen
Trace-Kontext und den fehlgeschlagenen Provider-Aufruf zeigen. Im Dashboard
erscheinen der nicht erfolgreiche Provider-Request sowie der Worker-Ausgang
`retry_scheduled`. Die korrelierte Logzeile enthält Trace-ID, Correlation-ID,
SyncJob-ID und den stabilen Fehlercode, aber keine Zugangsdaten oder externen
Accountnamen.

Nach dem Nachweis wird der Diagnose-Worker entfernt und der normale Worker ohne
Override neu gestartet. Ein noch fälliger Auftrag wird durch die
at-least-once-Verarbeitung erneut übernommen.

## Diagnose-Reihenfolge

1. Grafana-Alert und Sync-Ausgang im Dashboard prüfen.
2. Vom Worker-Trace zum automatischen HTTP-Client-Span wechseln.
3. Über die Trace-ID zu den Loki-Logs navigieren.
4. Bei `retry_scheduled` `Retry-After`, Versuchszahl und nächsten Zeitpunkt
   prüfen.
5. Bei `dead_lettered` Ursache beheben und erst danach den rollenbasierten
   Operator-Retry verwenden.
6. Bei `ReviewHopAuthenticatedRateLimitRejections` Client-Wiederholungen und
   legitime Last anhand des
   [Kapazitäts-Runbooks](capacity-resilience-runbook.md) unterscheiden; die
   Metrik enthält absichtlich keine Benutzerkennung.

## Hetzner

Nach expliziter Ressourcenfreigabe wird die bestehende Compose-Datei um
`deploy/hetzner/compose.observability.yml` ergänzt. Grafana bindet auch dort nur
an `127.0.0.1:3001` und wird über einen SSH-Tunnel erreicht:

```powershell
ssh -L 3001:127.0.0.1:3001 ankileet-admin@SERVER
```

Vor Aktivierung sind Arbeitsspeicher, Plattenwachstum und die 24-Stunden-
Aufbewahrung auf dem realen Server zu prüfen. Es findet kein automatischer
Hetzner-Apply statt.
