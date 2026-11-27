# ADR-0022: Produktname ReviewHop und technische Kompatibilität

- Status: Accepted
- Datum: 2026-11-25

## Kontext

Das Lernprojekt wurde als AnkiLeet entwickelt. Vor dem öffentlichen
Go-live erhält es einen eigenständigen Namen, der keine Verbindung zu
einer bestimmten Lernplattform suggeriert.

## Entscheidung

ReviewHop ist der Name für Oberfläche, Login-Anzeige, API-Dokumentation,
Portfolio und Betriebsdokumentation. Das Java-Paket und die Group ID
lauten `dev.reviewhop`. Neue Java- und npm-Builds sowie
Anwendungsimages verwenden den neuen Namen. Für Hetzner ist
`reviewhop.tongmilz.com` mit eigenen Login- und Admin-Hosts vorgesehen.

Der lokale OIDC-Issuer, Realm, Client-IDs, Zielgruppe und Operator-Rolle
bleiben stabil. Die Benutzerzuordnung hängt an Issuer und Subject.
Datenbanknamen, Rollen, Compose-Projekt, bestehende Flyway-Migrationen,
Konfigurationsvariablen und Metriknamen bleiben aus Gründen der
Kompatibilität erhalten. Zur Laufzeit bleibt der Keycloak-Realmname
`ankileet`, der Anzeigename neuer Realms lautet ReviewHop. In einem
bestehenden Realm kann der Anzeigename separat geändert werden.

Bestehende GitHub-Links, AWS-State- und Ressourcennamen sowie interne
Hetzner-Pfade und Backup-Kennungen werden weiterverwendet. Eine spätere
Änderung dieser Kennungen benötigt eine eigene Migration und Betriebsprüfung.

## Konsequenzen

Das Rebranding erfordert kein Zurücksetzen lokaler Daten. Neue Container
werden unter den neuen Image-Namen gebaut. Historische Nachweise behalten
ihre gültigen Links und die tatsächlich verwendeten Kennungen. Die
GitHub-Umbenennung und die öffentlichen DNS-Einträge werden separat
verwaltet.
