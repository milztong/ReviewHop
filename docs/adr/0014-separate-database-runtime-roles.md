# ADR-0014: Getrennte Datenbankrollen für Migration und Anwendung

- Status: Accepted
- Datum: 2026-11-20

## Kontext

Der Walking Skeleton verwendete ein gemeinsames PostgreSQL-Credential für Flyway und die laufende Anwendung. Damit könnte ein Fehler oder kompromittierter Anwendungspfad auch Schemaänderungen durchführen. TD-011 verlangt vor Phase 3 getrennte Least-Privilege-Rollen.

## Entscheidung

- `ankileet_migrator` besitzt das Datenbankschema und führt ausschließlich beim Backend-Start die versionierten Flyway-Migrationen aus.
- `ankileet_app` erhält nur `CONNECT`, Schema-`USAGE` sowie `SELECT`, `INSERT`, `UPDATE` und `DELETE` auf durch den Migrator erzeugten Tabellen.
- Eine additive Flyway-Migration vergibt die expliziten DML-Rechte auf Anwendungstabellen. Neue Tabellen müssen in derselben Änderung ausdrücklich freigegeben werden; die Anwendung erhält keine Rechte auf `flyway_schema_history`.
- Spring Datasource verwendet die Application-Rolle; `spring.flyway.user` und `spring.flyway.password` verwenden getrennt die Migrator-Rolle.
- Credentials werden nur über Umgebungsvariablen beziehungsweise den vorgesehenen Secret Store bereitgestellt. Die Beispielwerte sind Platzhalter.
- Eine bestehende lokale Phase-1-/Phase-2-Datenbank wird für den Phase-3-Entwicklungsstart bewusst neu initialisiert. Für persistente öffentliche Daten wäre stattdessen ein kontrollierter Rollen-Migrationsschritt erforderlich.

Damit ist TD-011 entschieden.

## Konsequenzen

Positiv:

- die laufende Anwendung kann keine DDL-Änderungen durchführen,
- Flyway bleibt alleinige Quelle für Schemaänderungen,
- die Rollentrennung ist lokal und auf Hetzner gleich konfiguriert.

Negativ:

- lokale und öffentliche Konfiguration benötigen zwei verschiedene Secrets,
- der Initialisierungsprozess muss vor den ersten Flyway-Migrationen laufen und neue Tabellen benötigen einen expliziten Grant,
- bestehende Volumes benötigen eine bewusste Migration oder lokale Neuerstellung.

## Verworfene Alternativen

### Gemeinsames Credential beibehalten

Dies verletzt Least Privilege und lässt der Anwendung unnötige Schemafähigkeiten.

### Migrationen manuell außerhalb des Deployments ausführen

Dies würde Reproduzierbarkeit und automatisierte Rollouts verschlechtern. Eine später eigenständige Migrator-Task bleibt möglich, falls mehrere Backend-Instanzen eingeführt werden.
