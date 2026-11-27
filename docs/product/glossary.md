# Glossar

Dieses Glossar definiert die verbindliche Fachsprache für Produkt, Dokumentation, API und Code. Codebezeichner verwenden die englischen Begriffe.

| Begriff | Codebegriff | Definition |
|---|---|---|
| ReviewHop-Benutzer | `AppUser` | Identität innerhalb von ReviewHop. Die Anmeldung wird durch einen externen OIDC-Provider bestätigt. |
| Provider | `Provider` | Externe Lernquelle wie Codewars oder eine manuelle Quelle. |
| Provider-Verbindung | `ProviderConnection` | Benutzerspezifische Verbindung zu einem Provider einschließlich Verbindungsart, Status und Sync-Fortschritt. |
| Connector | `ProviderConnector` | Adapter, der eine offizielle oder erlaubte Provider-Schnittstelle in das interne Importmodell übersetzt. |
| Quellinhalt | `SourceItem` | Providerbezogene Beschreibung eines externen Lerninhalts. Eindeutig durch Provider und externe ID. |
| Lerninhalt | `LearningItem` | Benutzerspezifische Zuordnung zu einem `SourceItem`, einschließlich Notizen, Abschlusszeitpunkt und Aktivierungsstatus. |
| Review Card | `ReviewCard` | Kleinste separat planbare Wiederholungseinheit eines `LearningItem`. |
| Review-Modus | `ReviewMode` | Art der Wiederholung, beispielsweise `APPROACH_RECALL` oder `FULL_RECODE`. |
| Review | `Review` | Ein konkreter Wiederholungsvorgang einer Review Card. |
| Review-Bewertung | `ReviewRating` | Selbsteinschätzung `AGAIN`, `HARD`, `GOOD` oder `EASY`. |
| Review-Protokoll | `ReviewLog` | Unveränderlicher Datensatz eines abgeschlossenen Reviews einschließlich vorherigem und neuem Scheduler-Zustand. |
| Fälligkeit | `DueAt` | Zeitpunkt, ab dem eine Review Card zur Wiederholung angeboten wird. |
| Scheduler | `SchedulingPolicy` | Fachliche Strategie, die aus Zustand, Bewertung und Zeitpunkt den nächsten Zustand berechnet. |
| FSRS | `FSRS` | Free Spaced Repetition Scheduler; geplante Scheduling-Strategie für Version 1. |
| gewünschte Retention | `DesiredRetention` | Zielwahrscheinlichkeit, einen Inhalt zum geplanten Zeitpunkt erinnern zu können. |
| Stability | `Stability` | Scheduler-Wert für die geschätzte Stabilität einer Erinnerung. |
| Difficulty | `Difficulty` | Scheduler-Wert für die geschätzte Schwierigkeit eines Lerninhalts für einen Benutzer. |
| Sync | `Sync` | Abgleich externer abgeschlossener Inhalte mit ReviewHop. |
| Sync-Auftrag | `SyncJob` | Nachverfolgbarer Auftrag für eine Provider-Synchronisation. |
| Sync-Cursor | `SyncCursor` | Providerspezifischer Fortschrittsmarker für inkrementellen oder paginierten Import. |
| Outbox Event | `OutboxEvent` | Persistiertes Integrationsereignis, das nach erfolgreicher Fachtransaktion asynchron veröffentlicht wird. |
| Idempotency Key | `IdempotencyKey` | Schlüssel, der die mehrfache Ausführung derselben Schreibabsicht verhindert. |
| Manuelle Quelle | `MANUAL` | Provider-Typ für vom Benutzer eingetragene Links und Metadaten ohne automatische Plattformverbindung. |
| Approach Recall | `APPROACH_RECALL` | Kurzer Review, bei dem Lösungsansatz, Datenstrukturen und Komplexität erinnert werden. |
| Aufgabenfrage | `recallPrompt` | Eigene Formulierung des Lernenden auf der Vorderseite der Review Card. |
| Full Recode | `FULL_RECODE` | Längerer Review, bei dem eine Aufgabe erneut vollständig bearbeitet wird. |

## Abgrenzungen

- Ein `SourceItem` gehört keiner einzelnen Person; ein `LearningItem` gehört genau einem Benutzer.
- Ein `LearningItem` ist keine `ReviewCard`: Ein Lerninhalt kann mehrere planbare Review-Modi besitzen.
- Ein `ReviewLog` wird nicht nachträglich überschrieben. Korrekturen erfolgen durch neue, nachvollziehbare Ereignisse.
- Ein Provider-Account und ein ReviewHop-Benutzer sind unterschiedliche Identitäten.
