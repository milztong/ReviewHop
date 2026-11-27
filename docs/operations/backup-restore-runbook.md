# Backup- und Restore-Runbook

Dieses Runbook gilt für die Hetzner-Portfolio-Demo. Es erzeugt keine
Cloudressourcen und ersetzt keine Kostenfreigabe. Architektur und Ziele sind in
[ADR-0020](../adr/0020-encrypted-postgresql-backups-with-restic.md) festgelegt.

## Betriebsziele

| Ziel | Wert |
|---|---|
| RPO | höchstens 24 Stunden |
| RTO | höchstens 60 Minuten |
| Rhythmus | täglich 03:15 UTC, bis 30 Minuten Jitter |
| Retention | 7 täglich, 4 wöchentlich, 3 monatlich |
| Inhalt | ReviewHop- und Keycloak-Datenbank als getrennte Dumps in einem Snapshot |
| Primärziel | privater Hetzner-Object-Storage-Bucket, andere deutsche Region als der Server |

## Vor der ersten externen Sicherung

1. Aktuellen Object-Storage-Preis, Serverregion, Zielregion, Verantwortlichen
   und Monatsbudget bestätigen. Das ist eine explizite Kostenfreigabe.
2. Ein Backup-eigenes Hetzner-Projekt ohne weitere Buckets verwenden und dort
   den S3-Zugang in der Hetzner Console erstellen. Er wird nicht committed und
   nur für den privaten Backup-Bucket verwendet. Hetzner-S3-Schlüssel gelten
   projektweit; die Projekttrennung stellt deshalb die praktisch kleinste
   Berechtigungsgrenze her. Hetzner bietet derzeit keine automatische Anlage
   dieser Zugangsdaten über die S3 API an.
3. `enable_backup_object_storage=true` setzen, eine vom Server abweichende
   `backup_object_storage_location` wählen und den Terraform-Plan prüfen. Der
   Bucket besitzt `prevent_destroy`; Apply und spätere Entfernung benötigen
   jeweils ausdrückliche Freigabe.
4. Auf dem Server `/opt/ankileet/secrets` für Benutzer `deploy` mit Modus `0700`
   anlegen und folgende Dateien im Eigentum dieses Benutzers mit Modus `0600`
   bereitstellen:

   - `backup-db-password`: Wert von `BACKUP_DB_PASSWORD`,
   - `restic-repository-password`: langes zufälliges Restic-Passwort,
   - `backup-s3-access-key` und `backup-s3-secret-key`,
   - `restore-verification-password`: separates zufälliges Einmal-/Drill-Passwort.

   Das Restic-Passwort zusätzlich getrennt von Server und Bucket in einem
   freigegebenen Passwortmanager verwahren. Keine dieser Dateien darf im
   Repository, Container-Image oder Journal erscheinen.
5. `RESTIC_REPOSITORY` auf
   `s3:https://REGION.your-objectstorage.com/BUCKET/ankileet/demo` setzen. Beim
   initialen Aufbau wird die Nur-Lese-Rolle `BACKUP_DB_USERNAME` durch das
   versionierte PostgreSQL-Init-Skript erstellt. Bei einem bereits existierenden
   Volume ist das Skript kontrolliert als Datenbankadministrator nachzuziehen.

## Repository initialisieren und ersten Lauf prüfen

```bash
cd /opt/ankileet
docker compose --env-file .env --file compose.yml --file compose.backup.yml \
  --profile operations run --rm backup /usr/local/bin/backup.sh init
docker compose --env-file .env --file compose.yml --file compose.backup.yml \
  --profile operations run --rm backup /usr/local/bin/backup.sh backup
```

`init` darf nur einmal gegen ein neues Ziel ausgeführt werden. Der Backup-Lauf
muss mit `backup_completed` enden. Repository- oder Zugangsdaten werden nicht in
die Ausgabe geschrieben.

## Täglichen Timer aktivieren

Als administrativer Benutzer:

```bash
install -m 0644 /opt/ankileet/systemd/ankileet-backup.service /etc/systemd/system/
install -m 0644 /opt/ankileet/systemd/ankileet-backup.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now ankileet-backup.timer
systemctl list-timers ankileet-backup.timer
```

Der letzte Lauf wird mit folgenden Befehlen geprüft:

```bash
systemctl status ankileet-backup.service
journalctl --unit ankileet-backup.service --since '2 days ago'
```

Ein fehlendes `backup_completed`, ein Fehler beim Integritätscheck oder ein
überschrittenes RPO ist ein Betriebsincident. Vor einem erneuten Lauf zuerst
Netzwerk, freien temporären Speicher, Datenbank-Readiness und Object-Storage-
Zugang prüfen; keine Locks oder Snapshots ungeprüft löschen.

## Isolierter Restore-Drill

Der Drill verwendet eine zweite PostgreSQL-Instanz auf einem flüchtigen
`tmpfs`. Er kann die laufenden Datenbanken nicht adressieren, solange der
versionierte Compose-Vertrag unverändert bleibt.

```bash
cd /opt/ankileet
docker compose --env-file .env --file compose.yml --file compose.backup.yml \
  --profile restore-drill run --rm restore-verifier \
  /usr/local/bin/restore-verify.sh
docker compose --env-file .env --file compose.yml --file compose.backup.yml \
  --profile restore-drill rm --force --stop restore-database
```

Erfolg bedeutet:

- beide Dump-Prüfsummen stimmen,
- fünf zentrale ReviewHop-Tabellen sind vorhanden,
- Realm `ankileet` und mindestens ein Client sind vorhanden,
- der Lauf endet mit `restore_verified`,
- die temporären Restore-Datenbanken und der Restore-Container werden entfernt.

Den Drill vor dem ersten öffentlichen Release, danach monatlich sowie nach
Änderungen an PostgreSQL-/Keycloak-Hauptversion oder Backupformat durchführen.
Zeitpunkt, Snapshot-ID, Dauer und Ergebnis dokumentieren, aber keine Benutzer-
oder Inhaltsdaten in das Protokoll übernehmen.

## Wiederanlauf nach Server- oder Datenbankneustart

Die produktiven Container verwenden `restart: unless-stopped`; API, Worker,
Frontend und Keycloak warten über Readiness-Abhängigkeiten auf ihre
Voraussetzungen. Nach einem ungeplanten Neustart:

```bash
cd /opt/ankileet
docker compose --env-file .env --file compose.yml ps
docker compose --env-file .env --file compose.yml logs --since 15m database keycloak backend worker frontend caddy
curl --fail --silent --show-error "https://${DOMAIN}/api/system-status"
systemctl list-timers ankileet-backup.timer
```

Die Demo erst wieder freigeben, wenn Datenbank und Keycloak bereit sind, der
BFF-Status `READY` meldet, ein Login möglich ist und der Timer aktiv bleibt.

## Echter Datenverlust

1. Schreibzugriffe stoppen und Incident-Zeitpunkt sowie letzten erfolgreichen
   Snapshot feststellen.
2. Nie direkt in eine bestehende Datenbank restaurieren. Zuerst den isolierten
   Drill gegen den gewählten Snapshot erfolgreich abschließen.
3. Auswirkungen des RPO sowie mögliche Lücke zwischen ReviewHop- und
   Keycloak-Dump bewerten.
4. Das Überschreiben oder Ersetzen produktrelevanter Daten benötigt eine eigene
   destruktive Benutzerfreigabe und einen dokumentierten Rückfallpunkt.
5. Nach Wiederherstellung Login, Zwei-Benutzer-Isolation, Due-Liste,
   Review-Historie, Provider-Sync und Outbox/Dead Letter prüfen.
