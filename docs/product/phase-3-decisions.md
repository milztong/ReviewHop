# Produktentscheidungen für Phase 3

- Datum: 2026-11-20
- Status: Verbindlich für den Phase-3-Scope

## PD-001 – Standardmäßige Review Card

Beim Aktivieren eines Coding-Lerninhalts wird genau eine `APPROACH_RECALL`-Review-Card erzeugt. `FULL_RECODE` wird nicht automatisch erzeugt und kann später bewusst zusätzlich aktiviert werden.

Begründung: Der kurze Abruf von Lösungsansatz, Datenstrukturen und Komplexität ist der kleinste regelmäßige Lernvorgang. Eine zweite automatische Karte würde die Due-Liste ohne ausdrückliche Lernabsicht verdoppeln.

## PD-002 – Daten eines abgeschlossenen Reviews

Die `ReviewRating` ist verpflichtend. Die Dauer berechnet der Server aus Start- und Abschlusszeitpunkt. Eine benutzereigene Notiz ist optional. Der Phase-3-Flow begrenzt eine gemeldete Reviewdauer auf 24 Stunden.

## PD-006 – Gespeicherte fremde Inhalte

ReviewHop speichert für fremde Lernplattformen nur externe ID, Titel, HTTPS-URL, Schwierigkeit und Tags. Notizen sind benutzereigene Inhalte. Vollständige fremde Aufgabenstellungen, Lösungen oder Sessiondaten werden nicht gespeichert.

Für die manuelle Quelle erzeugt ReviewHop eine interne externe ID aus Benutzer und Idempotency Key. Dadurch bleibt die Erstellung retry-sicher, ohne eine fremde Plattformidentität zu behaupten.
