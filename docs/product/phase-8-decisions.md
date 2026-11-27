# Phase-8-Produktentscheidungen

- Status: Verbindlich
- Datum: 2026-11-24
- Scope: PD-009 – Datenexport und vollständige Kontolöschung

## Entscheidung

Version 1 bietet jedem angemeldeten Benutzer:

1. einen maschinenlesbaren JSON-Export der eigenen ReviewHop-Fachdaten und
2. eine vollständige, idempotente Löschung dieser Fachdaten.

Der Export enthält Profilpräferenzen, Provider-Verbindungen, Learning Items
einschließlich der benötigten SourceItem-Metadaten, Review Cards, Review Logs
und SyncJobs. Interne OIDC-Kennungen, Idempotency Keys, Outbox-Payloads,
Trace-Kontext und rein technische Lease-Daten werden nicht exportiert.

Die Löschung entfernt in einer Datenbanktransaktion:

- den `AppUser`,
- dessen Provider-Verbindungen, SyncJobs und Outbox Events,
- dessen Learning Items, Review Cards und Review Logs,
- Source Items, die durch die Löschung verwaist sind.

Geteilte Source Items bleiben erhalten, solange ein anderer Benutzer sie
referenziert. Ein wiederholter Löschaufruf ist erfolgreich und erzeugt keinen
neuen `AppUser`.

## Abgrenzung zum OIDC-Account

ReviewHop verwaltet keine Passwörter und kann den Account beim externen
OIDC-Provider nicht über den normalen Anwendungszugriff löschen. Nach der
Fachdatenlöschung wird die lokale BFF-Sitzung beendet. Die Oberfläche weist
darauf hin, dass eine gegebenenfalls gewünschte Löschung des Keycloak-Accounts
ein separater Identity-Provider-Vorgang ist.

## Security- und Audit-Vertrag

- Export und Löschung sind nur für den aktuell authentifizierten Benutzer
  möglich; eine fremde Benutzer-ID wird nicht als Eingabe akzeptiert.
- Der Export wird als Download mit explizitem JSON-Vertrag geliefert und nicht
  in Logs oder Telemetrie kopiert.
- Erfolgreiche Exporte und Löschungen erzeugen strukturierte Audit-Logs und
  fachliche Metriken ohne Benutzer-ID, OIDC-Subject oder exportierte Inhalte.
- Die HTTP-Spans liefern den korrelierbaren Trace-Kontext.
- Die Löschung liefert absichtlich keine Detailzahlen an den Browser, damit
  wiederholte Aufrufe dieselbe beobachtbare Wirkung besitzen.

## Konsequenzen

- Review Logs bleiben im normalen Betrieb unveränderlich, werden aber bei der
  ausdrücklich angeforderten vollständigen Kontolöschung entfernt.
- Backups können gelöschte Daten gemäß TD-014 bis zu drei Monatsständen
  enthalten. Bei einem Restore dürfen bereits gelöschte Benutzerkonten nicht
  unbemerkt wieder veröffentlicht werden; der Recovery-Plan muss die seit dem
  Snapshot erfolgten Löschanforderungen berücksichtigen.
- Exportformat `1` ist versioniert. Spätere inkompatible Änderungen benötigen
  eine neue Formatversion.
