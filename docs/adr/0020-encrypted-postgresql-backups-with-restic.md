# ADR-0020: Verschlüsselte PostgreSQL-Backups mit Restic und getrenntem Object Storage

- Status: Accepted
- Datum: 2026-11-24

## Kontext

Die Hetzner-Demo hält ReviewHop-Fachdaten und Keycloak-Identitätsdaten in zwei
PostgreSQL-Datenbanken derselben Instanz. Providerseitige Server-Backups helfen
bei einem beschädigten Server, sind aber kein anwendungsbezogener,
portabler Restore-Nachweis. TD-014 verlangte daher ein verschlüsseltes Ziel
außerhalb des Demo-Servers, eine begrenzte Aufbewahrung und einen geprüften
Restore beider Datenbanken.

Das Verfahren muss lokal ohne laufende Cloudkosten testbar bleiben. Ein
kostenpflichtiges Ziel darf erst nach ausdrücklicher Freigabe entstehen.

## Entscheidung

- `pg_dump` erzeugt täglich getrennte Custom-Format-Dumps für die ReviewHop- und
  die Keycloak-Datenbank. Eine dedizierte PostgreSQL-Rolle mit
  `pg_read_all_data` und `CONNECT` auf genau diese Datenbanken führt den Dump
  aus; weder Anwendungsrolle noch Flyway-Migrator werden verwendet.
- Restic `0.20.0` verschlüsselt und authentifiziert beide Dumps, Manifest und
  SHA-256-Prüfsummen clientseitig als einen gemeinsamen Snapshot. Unverschlüsselte
  Zwischenstände liegen nur im temporären Dateisystem des kurzlebigen
  Backup-Containers.
- Das primäre externe Ziel ist ein privater Hetzner-Object-Storage-Bucket in
  einer deutschen Region, die sich von der Serverregion unterscheidet. Der
  Bucket wird optional und standardmäßig deaktiviert durch Terraform verwaltet.
  Das Erzeugen bleibt von Kostenfreigabe und Terraform-Apply abhängig.
- Lokal und in CI verwendet derselbe Restic-Vertrag ein temporäres
  Dateisystem-Repository. Die Anwendung und die Restore-Skripte enthalten keine
  providerabhängige Verzweigung; `RESTIC_REPOSITORY` wählt nur den Adapter.
- Ein systemd-Timer startet den Backup-Container täglich um 03:15 UTC mit bis
  zu 30 Minuten zufälliger Verzögerung. `Persistent=true` holt einen während
  eines ausgeschalteten Servers verpassten Lauf nach.
- Die Aufbewahrung beträgt sieben tägliche, vier wöchentliche und drei
  monatliche Snapshots. Nach jedem Lauf werden Repository-Integrität und
  Retention geprüft.
- Das Ziel ist RPO 24 Stunden und RTO 60 Minuten für die Portfolio-Demo.
- Ein Restore erfolgt zuerst ausschließlich in einer getrennten, temporären
  PostgreSQL-Instanz. Prüfsummen, zentrale ReviewHop-Tabellen, der Keycloak-Realm
  `ankileet` und mindestens ein zugehöriger Client müssen nachweisbar sein.
  Ein Restore über bestehende Demo-Daten benötigt eine eigene destruktive
  Freigabe und ist nicht Bestandteil des automatisierten Verfahrens.
- Repository-Passwort, Datenbankpasswort und S3-Zugang liegen in getrennten,
  nicht versionierten Secret-Dateien. Der Verlust des Restic-Passworts macht
  das Backup unbrauchbar; es muss daher getrennt vom Bucket und Server in einem
  freigegebenen Passwortmanager verwahrt werden.
- Die S3-Zugangsdaten stammen aus einem Backup-eigenen Hetzner-Projekt, das
  keine weiteren Buckets enthält. Damit wird die derzeit projektweite Wirkung
  eines Hetzner-S3-Schlüsselpaars praktisch auf das Backupziel begrenzt.

## Konsequenzen

- Das Backup ist auch bei Zugriff auf den Object-Storage-Bucket ohne das
  Restic-Passwort nicht lesbar.
- Ein isolierter Restore ist in CI reproduzierbar und kann gegen das reale
  externe Repository wiederholt werden.
- Logische Dumps ermöglichen einen portablen Restore, sind aber kein
  Point-in-Time-Recovery. Das RPO bleibt deshalb auf den täglichen Rhythmus
  begrenzt.
- Die zwei Dumps entstehen nacheinander und bilden keinen global atomaren
  Snapshot über beide Datenbanken. Die Anwendung referenziert Keycloak nur über
  stabile OIDC-Kennungen und toleriert daher den kurzen Zeitversatz für die
  Demo. Ein produktiver Mehrmandantenbetrieb müsste diese Annahme neu bewerten.
- Object Storage verursacht wiederkehrende Kosten und wird nicht durch diese
  Entscheidung provisioniert. Bis Freigabe, Apply und realem Restore-Drill
  bleibt der öffentliche Release blockiert.

## Verworfene Alternativen

- **Nur Hetzner-Server-Backups:** kein logischer, portabler Restore und keine
  unabhängige Aufbewahrungsprüfung.
- **Unverschlüsselte Dumps auf dem Demo-Server:** derselbe Ausfallbereich und
  unzureichender Schutz personenbezogener Daten.
- **AWS S3 als primäres Ziel:** technisch über den Restic-S3-Adapter möglich,
  aber ohne Vorteil gegenüber der gewählten Hetzner-Primärplattform.
- **Kontinuierliches WAL-Archiv/PITR:** für Größe und RPO der Portfolio-Demo
  unverhältnismäßige Betriebs- und Restore-Komplexität.

## Verweise

- [ADR-0012](0012-hetzner-primary-demo-platform.md)
- [ADR-0014](0014-separate-database-runtime-roles.md)
- [Backup- und Restore-Runbook](../operations/backup-restore-runbook.md)
