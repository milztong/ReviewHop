# ADR-0016: Konsistenter synchroner Codewars-Sync in Phase 5

- Status: Accepted
- Datum: 2026-11-21

## Kontext

Codewars API v1 bietet ein öffentliches Profil und eine nullbasiert paginierte
Liste abgeschlossener Kata mit bis zu 200 Einträgen pro Seite. Die offizielle
Dokumentation garantiert keine Sortierreihenfolge und beschreibt die API selbst
als minimal und inkonsistent. Phase 5 benötigt dennoch einen sichtbaren,
idempotenten manuellen Import; asynchrone Ausführung folgt erst in Phase 6.

## Entscheidung

Der Codewars-Adapter implementiert den capability-basierten `ProviderConnector`-
Port mit `PUBLIC_PROFILE`, `MANUAL_IMPORT` und `INCREMENTAL_SYNC`.

Für Phase 5 gilt:

1. Der Benutzer startet den Sync explizit; der Request läuft synchron.
2. Externe HTTP-Aufrufe liegen außerhalb von Datenbanktransaktionen.
3. Jede vollständig gelesene Seite wird in einer kurzen Transaktion importiert.
4. `SourceItem` ist über `(provider, externalId)` global eindeutig und
   `LearningItem` über `(userId, sourceItemId)` benutzerbezogen eindeutig.
5. Bereits vorhandene LearningItems, Notizen, Review Cards und ReviewLogs werden
   durch einen erneuten Import nicht überschrieben.
6. Wegen der fehlenden Sortiergarantie werden alle von Codewars gemeldeten Seiten
   gelesen. Der Cursor ist ein High-Water-Mark des letzten vollständig erfolgreichen
   Snapshots; er dient noch nicht zum vorzeitigen Abbruch der Pagination.
7. Der Verbindungscursor wird nur nach einem vollständig erfolgreichen Lauf
   verschoben. Ein Teilfehler lässt ihn unverändert; bereits importierte Seiten
   bleiben konsistent und werden beim nächsten Lauf idempotent wiedererkannt.
8. Timeouts, Netzwerkfehler, HTTP `429` und `5xx` werden als retryable klassifiziert.
   Ungültige Antworten und sonstige `4xx` sind terminal. Phase 5 führt keine
   automatischen In-Request-Retries aus; ein Benutzer kann einen neuen Sync starten.
9. Ein Sync liest höchstens 1.000 Seiten als Sicherheitsgrenze.
10. Pro ProviderConnection darf nur ein `RUNNING`-SyncJob existieren. Ein nach
    Prozessabbruch 30 Minuten lang nicht abgeschlossener Job wird beim nächsten
    Start als retryable `STALE_SYNC` beendet; terminale Jobs können nicht erneut
    abgeschlossen werden.

`INCREMENTAL_SYNC` bezeichnet hier die fachlich inkrementelle Übernahme neuer
LearningItems mit persistiertem Fortschrittsmarker. Eine API-Aufrufoptimierung durch
vorzeitiges Beenden ist erst zulässig, wenn Codewars eine belastbare Sortier- oder
Cursorsemantik dokumentiert.

## Konsequenzen

Positiv:

- Wiederholte oder teilweise wiederholte Läufe erzeugen keine Duplikate.
- Providerfehler sind als `SyncJob` sichtbar und bestehende Daten bleiben nutzbar.
- Phase 6 kann dieselben Ports und Zustände in einen Worker verschieben.
- Die feste Codewars-Basisadresse verhindert benutzergesteuerte Ziel-URLs.

Negativ:

- Jeder Phase-5-Sync liest alle Seiten und verbraucht mehr API-Aufrufe als ein
  sortiergarantierter Delta-Sync.
- Ein synchroner Request kann bei großen Profilen länger dauern; dies wird in
  Phase 6 durch asynchrone Verarbeitung ersetzt.
- Es gibt innerhalb eines Requests keine automatischen Retries.

## Verworfene Alternativen

### Vorzeitiger Abbruch am ersten bekannten Element

Ohne dokumentierte Sortierreihenfolge könnte dies neue oder verschobene Einträge
überspringen.

### Eine große Transaktion über HTTP-Aufrufe und alle Seiten

Dies würde Datenbankverbindungen während externer Wartezeit binden und die
Fehleranfälligkeit erhöhen.

### Automatische Retries im Benutzerrequest

Dies verlängert blockierende Requests und kann Rate Limits verschärfen. Phase 6
übernimmt begrenzte Retries mit Backoff/Jitter im Worker.

## Referenzen

- [ADR-0003](0003-provider-connector-strategy.md)
- [Codewars API Reference](https://dev.codewars.com/)
- [Phase-5-Produktentscheidungen](../product/phase-5-decisions.md)
