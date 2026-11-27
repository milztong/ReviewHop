# ADR-0002: Clean/Hexagonal Architecture und Package by Feature

- Status: Accepted
- Datum: 2026-11-19

> Die Architekturentscheidung bleibt gültig. Die Beispiele AWS, SQS und KMS sind seit [ADR-0012](0012-hetzner-primary-demo-platform.md) optionale Adapter statt Vorgaben der primären Demo-Plattform.

## Kontext

Das Projekt soll Spring Boot, JPA, externe Provider und AWS verwenden, ohne den Fachkern dauerhaft an diese Technologien zu binden. Gleichzeitig darf Architekturdisziplin nicht zu einer Schicht aus spekulativen Interfaces und Mapping-Code ohne Nutzen führen.

## Entscheidung

Fachliche Module werden per Package by Feature organisiert. Innerhalb eines Moduls gelten Ports-and-Adapters:

- Domain: Fachzustand und Regeln, reines Java
- Application: Use Cases und benötigte Ports
- Inbound Adapter: REST, Event Consumer
- Outbound Adapter: JPA, HTTP, SQS, KMS
- Configuration: Framework-Verdrahtung

Abhängigkeiten zeigen zum Fachkern. Ports werden nur an tatsächlichen Prozess- oder Technologiegrenzen eingeführt. Constructor Injection, Immutability und kleine kohäsive Verträge sind Standard.

ArchUnit und Spring Modulith prüfen die Regeln automatisiert.

## Konsequenzen

Positiv:

- Fachlogik ist schnell und deterministisch testbar,
- Provider, Scheduler und Cloudadapter bleiben austauschbar,
- Modulgrenzen werden im Code sichtbar,
- Framework-Upgrades beeinflussen den Kern weniger.

Negativ:

- getrennte Domain- und Persistenzmodelle können Mapping-Aufwand erzeugen,
- Entwickler müssen Grenzfälle bewusst einem Modul zuordnen,
- übertriebene Portbildung würde Lesbarkeit verschlechtern und ist deshalb ausdrücklich zu vermeiden.

## Verworfene Alternativen

### Klassische globale Layer

Globale Pakete für Controller, Services und Repositories verschleiern Fachgrenzen mit wachsendem Projekt.

### JPA-Entitäten als vollständiges Domainmodell

Dies koppelt den Kern an Persistenzlebenszyklus und Framework-Anforderungen. Für einfache Read Models kann pragmatisch abgewichen werden, wenn die Abhängigkeit nicht in den Domainkern gelangt.
