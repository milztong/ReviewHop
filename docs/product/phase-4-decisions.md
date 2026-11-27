# Produktentscheidungen für Phase 4

- Datum: 2026-11-21
- Status: Verbindlich für den Phase-4-Scope

## PD-003 – Vorgezogene Reviews

Review Cards dürfen vor ihrer Fälligkeit bearbeitet werden. Der tatsächliche
Review-Zeitpunkt wird als Zeitbasis an den Scheduler übergeben; die Anwendung
verschiebt ihn nicht künstlich auf den bisherigen Fälligkeitszeitpunkt.

Begründung: Benutzer können bewusst früher üben. FSRS unterstützt abweichende
Review-Zeitpunkte, solange der reale Zeitpunkt und der vorherige Zustand
vollständig vorliegen.

## PD-004 – Gewünschte Retention

Jeder Benutzer startet mit einer gewünschten Retention von `0,90`. Der Wert
wird serverseitig validiert und bei jedem Review im ReviewLog festgehalten.
Eine öffentliche Einstellung zum Ändern des Werts ist nicht Teil von Phase 4.

Begründung: 0,90 ist der etablierte Ausgangswert der gewählten FSRS-Implementierung
und balanciert Erinnerungswahrscheinlichkeit und Review-Aufwand. Die Speicherung
pro Benutzer hält eine spätere Konfiguration ohne Scheduler-Umbau offen.
