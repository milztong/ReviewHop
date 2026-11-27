# ADR-0007: SQS und Transactional Outbox vor Kafka

- Status: Superseded
- Datum: 2026-11-19

> [ADR-0012](0012-hetzner-primary-demo-platform.md) öffnete die Transportwahl
> erneut. [ADR-0017](0017-postgresql-outbox-worker.md) entscheidet PostgreSQL
> als primären Outbox-Transport für Hetzner; SQS bleibt optional.

## Kontext

Provider-Synchronisation benötigt asynchrone Ausführung, Retries und Dead-Letter-Behandlung. Das Portfolio soll architektonische Urteilskraft zeigen und nicht jede bekannte Technologie ohne fachlichen Bedarf einsetzen. Kafka gehört bereits zum vorhandenen Erfahrungsprofil; AWS und Terraform sind die wichtigeren neuen Schwerpunkte.

## Entscheidung

Arbeitsaufträge für Provider-Synchronisationen werden zunächst über AWS SQS transportiert. Fachänderungen, die zuverlässig ein Integrationsereignis auslösen, verwenden eine Transactional Outbox in PostgreSQL.

Consumer werden für mindestens einmalige Zustellung und idempotente Verarbeitung entworfen. Eine Dead-Letter-Queue nimmt endgültig fehlgeschlagene Aufträge auf.

Kafka wird erst eingeführt, wenn mehrere unabhängige Event-Consumer, Replay, hohe Ereignisvolumen oder Stream-Verarbeitung einen belegbaren Nutzen erzeugen. Fachliche Events und Transport werden dafür voneinander getrennt.

## Konsequenzen

Positiv:

- gute Integration mit der AWS-Zielplattform,
- einfache Retry- und DLQ-Semantik,
- weniger Infrastruktur- und Kostenaufwand als Managed Kafka,
- Outbox verhindert das Dual-Write-Problem zwischen Datenbank und Queue.

Negativ:

- SQS besitzt kein allgemeines Event-Replay wie ein persistentes Kafka-Log,
- Outbox Publisher und Bereinigung benötigen Betrieb und Monitoring,
- ein späterer Transportwechsel erfordert neue Adapter und Migration.

## Verworfene Alternativen

### Kafka ab Phase 1

Der aktuelle Ablauf benötigt zunächst eine Arbeitswarteschlange, keinen verteilten Event-Streaming-Backbone.

### Direkter SQS-Publish innerhalb der Fachtransaktion

Ein Fehler zwischen Datenbank-Commit und Queue-Publish könnte Zustand und Nachricht auseinanderlaufen lassen.

### Reine Datenbank-Jobtabelle ohne Queue

Sie wäre für den lokalen Start möglich, würde aber den geplanten AWS-Workerpfad und dessen Retry-/DLQ-Eigenschaften nicht abbilden.
