# ADR-0017: PostgreSQL-Outbox als Hetzner-Arbeitswarteschlange

- Status: Accepted
- Datum: 2026-11-22
- Ersetzt: Transportfestlegung aus ADR-0007

## Kontext

Phase 6 verschiebt Provider-Synchronisationen aus dem HTTP-Request in einen
separaten Worker. Die Hetzner-Demo betreibt bereits PostgreSQL auf demselben
Server. Ein zusätzlicher Broker wie RabbitMQ, Redis oder Kafka würde Speicher,
Patch- und Backupaufwand erhöhen, obwohl die erwartete Demo-Last klein ist.

Die Transactional Outbox bleibt verbindlich: Das Annehmen eines `SyncJob` und
das Erzeugen seines Arbeitsauftrags dürfen nicht durch einen Dual Write zwischen
Datenbank und Broker auseinanderfallen. Consumer müssen mindestens einmalige
Zustellung, Prozessabbrüche und begrenzte Providerfehler vertragen.

## Entscheidung

Für die primäre Hetzner- und lokale Umgebung dient PostgreSQL als persistenter
Outbox-Transport:

1. Die API persistiert `SyncJob` im Zustand `REQUESTED` und genau ein
   `SyncRequested`-Outbox-Event in derselben Transaktion.
2. Ein separat gestarteter Spring-Boot-Worker verwendet dasselbe versionierte
   Anwendungsimage mit dem Runtime-Modus `worker`. Er bietet keinen HTTP-Port an.
3. Mehrere Worker dürfen fällige Events konkurrierend mit
   `FOR UPDATE SKIP LOCKED` leasen. Provider-HTTP-Aufrufe erfolgen außerhalb der
   Lease-Transaktion.
4. Ein Lease besitzt eine begrenzte Gültigkeit. Nach Prozessabbruch wird das
   Event erneut zustellbar. Der Worker erneuert das Lease nach jeder vollständig
   importierten Seite. Fachliche Idempotenz im Sync und Learning-Import schützt
   vor doppelter Wirkung.
5. Retryable Fehler erhalten höchstens fünf Zustellversuche mit exponentiellem
   Backoff und Jitter. Ein größeres `Retry-After` des Providers wird respektiert.
6. Nach ausgeschöpften Versuchen wird das Event `DEAD_LETTERED`; der zugehörige
   SyncJob erhält denselben sichtbaren Zustand. Terminale Providerantworten
   beenden den Sync ohne technischen Retry.
7. Eine nur für die Operator-Rolle zugängliche Aktion darf ein
   `DEAD_LETTERED`-Event erneut in `PENDING` stellen. Der Vorgang wird ohne
   Provider-Credentials und ohne personenbezogene Fehlerdetails protokolliert.
8. Outbox-Payloads sind versionierte JSON-Verträge und enthalten nur interne
   IDs, Eventtyp, Correlation-/Trace-Kontext und Erstellungszeitpunkt.
9. Verarbeitete Events werden zunächst aufbewahrt. Aufbewahrung und technische
   Bereinigung werden vor dem öffentlichen Release betrieblich festgelegt.

Der Worker-Poller ist zugleich der Scheduler für fällige Retries. Eine
automatische periodische Benutzersynchronisation gehört noch nicht zum
Produktumfang. AWS SQS bleibt ein optionaler Transportadapter; EventBridge kann
in einem optionalen AWS-Deployment später Zeitimpulse liefern. Kafka bleibt ohne
zusätzlichen Streaming-Anwendungsfall ausgeschlossen.

## Konsequenzen

Positiv:

- keine zusätzliche laufende Infrastruktur oder Cloudkosten auf Hetzner,
- atomare Annahme von SyncJob und Arbeitsauftrag,
- lokal, in CI und auf Hetzner identisches Zustellverhalten,
- horizontale Worker-Konkurrenz ohne doppelte gleichzeitige Beanspruchung,
- Transport bleibt hinter einem kleinen Port gegen SQS austauschbar.

Negativ:

- Polling erzeugt regelmäßige Datenbankzugriffe,
- Queue und Fachdaten teilen Ausfall- und Kapazitätsgrenzen,
- PostgreSQL ersetzt keinen hochvolumigen Message Broker,
- Leasing, Backoff, Dead Letter und Bereinigung müssen selbst betrieben und
  beobachtet werden.

## Verworfene Alternativen

### RabbitMQ oder Redis auf demselben Server

Beide ergänzen einen weiteren zustandsbehafteten Dienst, ohne für die erwartete
Last einen belegten Nutzen zu liefern.

### SQS als primärer Transport

SQS ist für das optionale AWS-Deployment geeignet, würde die dauerhaft laufende
Hetzner-Demo aber unnötig an ein zweites kostenpflichtiges Cloudkonto koppeln.

### Nur eine Jobtabelle ohne Outbox-Vertrag

Damit wären Arbeitsauftrag, Payload-Version, Zustellversuche und Dead-Letter-
Lebenszyklus nicht sauber von der fachlichen SyncJob-Sicht getrennt.

## Referenzen

- [ADR-0001](0001-modular-monolith-and-worker.md)
- [ADR-0007](0007-sqs-outbox-and-eventing.md)
- [ADR-0012](0012-hetzner-primary-demo-platform.md)
- [ADR-0016](0016-codewars-sync-consistency.md)

