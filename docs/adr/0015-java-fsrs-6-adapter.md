# ADR-0015: Java-FSRS 1.0.0 als versionierter FSRS-6-Adapter

- Status: Accepted
- Datum: 2026-11-21

## Kontext

ADR-0004 verlangt eine verifizierte FSRS-Implementierung hinter dem
`SchedulingPolicy`-Port. TD-001 ließ offen, ob ReviewHop den Algorithmus selbst
implementiert oder eine Bibliothek verwendet. Eine Eigenimplementierung würde
mathematisch sensiblen Code duplizieren und einen hohen Referenztest-Aufwand
erzeugen.

Die vom Open-Spaced-Repetition-Projekt gepflegte Bibliothek `java-fsrs` stellt
FSRS-6 für Java 17+ bereit, erlaubt einen expliziten Review-Zeitpunkt und ist
unter MIT lizenziert. Version 1.0.0 verwendet 21 Modellparameter und kann ohne
Intervall-Fuzzing deterministisch betrieben werden.

## Entscheidung

ReviewHop verwendet `io.github.open-spaced-repetition:fsrs:1.0.0` ausschließlich
im Scheduling-Ausgangsadapter. Bibliothekstypen dürfen den Adapter nicht
verlassen. Domain und Application verwenden eigene unveränderliche
`SchedulingSnapshot`- und `DesiredRetention`-Typen.

Die erste Parameterversion wird als `fsrs-6-java-1.0.0-defaults-v1` gespeichert.
Sie besteht aus:

- den mit Java-FSRS 1.0.0 veröffentlichten 21 Standardparametern,
- einem Lernschritt von zehn Minuten,
- einem Wiederlernschritt von zehn Minuten,
- maximal 36.500 Tagen Intervall,
- deaktiviertem Fuzzing für reproduzierbare Ergebnisse.

Der Review-Zeitpunkt und die gewünschte Retention werden explizit übergeben.
Der Adapter liest keine Systemzeit. Jeder ReviewLog speichert die verwendete
Version, Retention sowie den Scheduler-Zustand vor und nach dem Review.

## Konsequenzen

Positiv:

- Die FSRS-Formeln werden nicht lokal dupliziert.
- Die Bibliothek bleibt durch den Port austauschbar.
- Ergebnisse sind mit festen Parametern und ohne Fuzzing reproduzierbar.
- Upgrades erhalten eine neue Parameterversion und können gegen alte Logs
  verglichen werden.

Negativ:

- ReviewHop übernimmt eine zusätzliche Laufzeitabhängigkeit.
- Die Bibliothek bietet keinen Parameter-Optimizer; Version 1 verwendet deshalb
  die veröffentlichten Defaults.
- Ein Upgrade erfordert Referenztests und eine explizite Kompatibilitätsentscheidung.

## Migration bestehender Phase-3-Daten

Review Cards ohne FSRS-Zustand beginnen beim nächsten Review im Zustand
`LEARNING`. Alte `phase3-simple-v1`-Logs bleiben unverändert und besitzen keine
nachträglich erfundenen FSRS-Snapshots. Neue FSRS-Logs enthalten vollständige
Vorher-/Nachher-Snapshots.

## Verworfene Alternativen

### Eigene FSRS-Implementierung

Sie erhöht das Fehlerrisiko im fachlichen Kern, ohne ReviewHop einen eigenen
Produktvorteil zu geben.

### Bibliothekstypen als Domainmodell

Das würde den Fachkern an eine instabile technische Abhängigkeit koppeln und
gegen ADR-0004 sowie die Clean-Architecture-Regeln verstoßen.

## Referenzen

- [Java-FSRS](https://github.com/open-spaced-repetition/java-fsrs)
- [FSRS-Algorithmus](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm)
- [ADR-0004](0004-fsrs-behind-scheduling-port.md)
