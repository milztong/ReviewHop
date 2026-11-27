# ADR-0004: FSRS hinter einem Scheduling-Port

- Status: Accepted
- Datum: 2026-11-19

## Kontext

Spaced Repetition ist Kernfunktion des Produkts. Der Algorithmus muss korrekt, testbar, versionierbar und später austauschbar sein. FSRS modelliert Schwierigkeit, Stabilität und Erinnerbarkeit moderner als ein statischer Intervallplan. Eine endgültige Java-Implementierung ist noch auszuwählen und gegen Referenzwerte zu prüfen.

## Entscheidung

Der Domainkern definiert einen `SchedulingPolicy`-Port. Version 1 verwendet eine verifizierte FSRS-Implementierung hinter diesem Port.

Jeder ReviewLog speichert mindestens:

- verwendete Scheduler- und Parameterversion,
- Bewertung und Review-Zeitpunkt,
- vorherigen und resultierenden Scheduling-Zustand.

Scheduling ist deterministisch: aktuelle Zeit, Parameter und Zufallsquelle werden explizit übergeben. Der Algorithmus führt kein I/O aus.

## Konsequenzen

Positiv:

- Referenztests können ohne Spring und Datenbank laufen,
- Algorithmus-Upgrades bleiben nachvollziehbar,
- alternative Strategien und Migrationen sind möglich,
- historische Entscheidungen lassen sich erklären.

Negativ:

- Algorithmus- und Parameterversionen erhöhen das Datenmodell,
- eine Java-Implementierung muss sorgfältig verifiziert werden,
- Änderungen am Algorithmus benötigen Migrations- oder Kompatibilitätsentscheidungen.

## Offener Folgepunkt

TD-001 entscheidet, ob eine bestehende Java-Bibliothek verwendet oder eine eigene Implementierung anhand offizieller Referenzdaten verifiziert wird.

## Verworfene Alternativen

### Fest kodierter einfacher Intervallplan als Produktalgorithmus

Er würde den fachlichen Anspruch nicht erfüllen und spätere Migration erschweren.

### FSRS direkt in Application Services

Dies würde Algorithmus und Use-Case-Orchestrierung vermischen.

## Referenzen

- [Anki Manual – Deck Options und FSRS](https://docs.ankiweb.net/deck-options.html)
- [Open Spaced Repetition – FSRS](https://github.com/open-spaced-repetition/free-spaced-repetition-scheduler)
