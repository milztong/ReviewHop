# ADR-0018: Selbst gehosteter LGTM-Stack hinter OpenTelemetry

- Status: Accepted
- Datum: 2026-11-23

## Kontext

Phase 7 benötigt Ende-zu-Ende-Traces, Metriken, strukturierte Logs,
Dashboards und Alerts. Die lokale Entwicklung muss kostenfrei bleiben. Für die
dauerhafte Hetzner-Demo darf Observability weder zusätzliche Cloudanbieter noch
unbemerkt zusätzliche Kosten voraussetzen. Der einzelne Demo-Server besitzt
zugleich eine begrenzte Speicher- und CPU-Kapazität.

API und Worker sollen nicht direkt an ein bestimmtes Backend gekoppelt werden.
Insbesondere der asynchrone Weg vom HTTP-Request über PostgreSQL-Outbox und
Worker bis zum Provider muss denselben Trace fortsetzen können.

## Entscheidung

1. Spring Boot instrumentiert Traces über Micrometer und OpenTelemetry. API und
   Worker exportieren OTLP an einen OpenTelemetry Collector.
2. Prometheus speichert technische und fachliche Metriken. Tempo speichert
   Traces, Loki strukturierte JSON-Logs und Grafana stellt Datenquellen,
   Dashboard und Alerts bereit.
3. Anwendungen schreiben strukturierte ECS-JSON-Logs auf die Standardausgabe.
   Grafana Alloy entdeckt und liest ausschließlich Containerlogs über einen
   Docker-Socket-Proxy, der nur die dafür benötigten lesenden Container-API-
   Endpunkte freigibt. Alloy selbst erhält keinen Docker-Socket.
4. Der vollständige Stack läuft lokal über die ausdrücklich ergänzte
   `compose.observability.yml`. Er verwendet lokale Volumes, begrenzte
   Aufbewahrung und keine externen SaaS-Dienste. Die Basiskonfiguration bleibt
   ohne Observability-Zugangsdaten startbar.
5. Auf Hetzner ist derselbe Stack optional auf demselben Server vorgesehen.
   Er wird erst aktiviert, wenn der reale Ressourcenbedarf gegen den gewählten
   Servertyp geprüft und ausdrücklich freigegeben wurde. Es entsteht dadurch
   kein weiterer Cloudvertrag.
6. Grafana wird weder lokal noch auf Hetzner über Caddy veröffentlicht. Lokal
   bindet es ausschließlich an `127.0.0.1`; auf Hetzner erfolgt administrativer
   Zugriff bei Aktivierung über einen SSH-Tunnel.
7. Der Collector akzeptiert OTLP ausschließlich im internen Compose-Netz.
   Loki, Tempo und Prometheus erhalten keine öffentlichen Host-Ports.
8. Metrik-Labels bleiben niedrig kardinal. Benutzer-IDs, externe Accountnamen,
   Tokens und vollständige Fehlertexte sind in Labels, Logs und Traces
   verboten. Interne SyncJob- und Correlation-IDs dürfen nur in Logs erscheinen.
9. Technische Alerts decken fehlende Telemetrie und wiederholte Providerfehler
   ab. Fachliche Alerts decken Dead-Letter-Syncs ab. Ein Alert besitzt zunächst
   eine sichtbare Grafana-Zustandsauswertung ohne externen
   Benachrichtigungskanal.

## Konsequenzen

Positiv:

- kostenfreier lokaler Betrieb und kein zusätzlicher Cloud-Lock-in,
- derselbe Telemetrievertrag für lokal, Hetzner und optional AWS,
- nachvollziehbarer API-Outbox-Worker-Provider-Trace,
- Logsammlung mit eingegrenztem Docker-API-Zugriff für Alloy,
- Dashboards und Alerts sind versioniert und reproduzierbar.

Negativ:

- der vollständige Stack benötigt mehrere zusätzliche Container und Volumes,
- Observability teilt auf Hetzner die Ausfall- und Kapazitätsgrenze des
  Anwendungsservers,
- Grafana ist absichtlich kein öffentliches Portfolio-Frontend,
- Docker hält den Standardausgabestrom bis zur Abholung vor; Rotation und
  Aufbewahrung der Containerlogs benötigen eine Betriebsgrenze.

## Verworfene Alternativen

### Externes Grafana Cloud Backend

Es vereinfacht den Betrieb, führt aber ein weiteres Konto, externe
Datenübertragung und mögliche spätere Kosten ein. Der OTLP-Vertrag lässt diese
Alternative später als Exportziel zu.

### Direkter Docker-Socket für Alloy

Ein Collector mit direktem Docker-Socket kann Container automatisch entdecken,
erhält aber weitreichenden Zugriff auf den Docker-Daemon. Der vorgeschaltete
Socket-Proxy begrenzt Alloys API-Sicht auf Container-Metadaten und -Logs.

### Observability auf einem zweiten Hetzner-Server

Die Trennung verbessert die Ausfallisolierung, verdoppelt für die Demo aber
nahezu die laufenden Serverkosten. Sie bleibt eine spätere Skalierungsoption.

## Referenzen

- [ADR-0012](0012-hetzner-primary-demo-platform.md)
- [ADR-0017](0017-postgresql-outbox-worker.md)
- [C4-Containerdiagramm](../architecture/container-diagram.md)
