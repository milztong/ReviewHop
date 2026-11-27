# Produktentscheidungen für Phase 5

- Datum: 2026-11-21
- Status: Verbindlich für den Phase-5-Scope

## PD-005 – Öffentliche Codewars-Profilzuordnung

Ein Benutzer verbindet Codewars durch Eingabe eines öffentlichen Benutzernamens.
ReviewHop löst den Namen über die offizielle öffentliche API zum kanonischen
Codewars-Benutzernamen auf und speichert die Zuordnung ausdrücklich als
`UNVERIFIED`.

Die Oberfläche behauptet keine Account-Inhaberschaft. ReviewHop fordert weder
Codewars-Passwörter noch API-Tokens oder Session-Cookies an und speichert solche
Daten nicht. Mehrere ReviewHop-Benutzer können deshalb dasselbe öffentliche Profil
zuordnen; ihre LearningItems und Reviewdaten bleiben trotzdem strikt getrennt.

Begründung: Codewars API v1 ist öffentlich und benötigt keine Authentifizierung.
Sie bietet jedoch keinen Nachweis, dass der ReviewHop-Benutzer das angegebene
Codewars-Konto kontrolliert.

## Gespeicherte Codewars-Daten

Phase 5 speichert nur die für Wiederholungen notwendigen öffentlichen Metadaten:

- kanonischer öffentlicher Benutzername,
- Kata-ID, Titel und kanonischer HTTPS-Link,
- Abschlusszeitpunkt.

Aufgabenbeschreibungen, Lösungen, Passwörter, Tokens und Sessiondaten werden nicht
gespeichert. Die Liste abgeschlossener Kata enthält keine Schwierigkeit; Phase 5
erzeugt deshalb keine zusätzlichen Einzelabfragen nur zur Anreicherung.
`completedLanguages` ist benutzerspezifisch und wird nicht als globales
`SourceItem`-Tag fehlmodelliert.
