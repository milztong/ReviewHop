# ADR-0001: Modularer Monolith mit separatem Sync-Worker

- Status: Accepted
- Datum: 2026-11-19

## Kontext

ReviewHop besitzt mehrere fachliche Bereiche, aber zunächst ein kleines Team und einen gemeinsamen Produktlebenszyklus. Externe Provider-Synchronisationen sind langsam, rate-limitiert und fehleranfällig. Sie dürfen API-Requests nicht blockieren.

## Entscheidung

Der fachliche Kern wird als modularer Monolith umgesetzt. Eine Spring-Boot-API bedient synchrone Use Cases. Ein separat deploybarer Spring-Boot-Worker verarbeitet asynchrone Provider-Synchronisationen.

API und Worker dürfen gemeinsame fachliche Module und dieselbe PostgreSQL-Datenbank verwenden. Fachliche Modulgrenzen werden dennoch explizit definiert und automatisiert geprüft.

## Konsequenzen

Positiv:

- einfache lokale Entwicklung und atomare Fachtransaktionen,
- geringer Betriebsaufwand,
- klare fachliche Grenzen ohne verteilte Transaktionskomplexität,
- langsame Provider-Aufrufe bleiben vom Request-Pfad getrennt.

Negativ:

- Deployment-Einheiten sind nicht vollständig unabhängig versionierbar,
- fehlerhafte Modulgrenzen könnten zu einem unstrukturierten Monolithen führen,
- Worker und API teilen Datenhoheit und müssen kompatibel deployt werden.

## Verworfene Alternativen

### Microservices pro Fachbereich

Für den aktuellen Umfang wären zusätzliche Netzwerkschnittstellen, Datenbanken, Deployments und verteilte Konsistenz unverhältnismäßig.

### Nur ein Prozess mit synchronem Provider-Zugriff

Dies würde Request-Latenz, Timeouts und Fehlerkopplung erhöhen.
