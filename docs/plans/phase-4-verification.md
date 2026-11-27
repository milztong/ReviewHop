# Phase-4-Verifikationsnachweis

- Datum: 2026-11-21
- Status: Abgeschlossen

## Gelieferter Scope

- Java-FSRS 1.0.0 als FSRS-6-Ausgangsadapter hinter `SchedulingPolicy`
- eigener frameworkfreier Domainzustand für Stability, Difficulty, State,
  Lernschritt, Fälligkeit und letzten Review-Zeitpunkt
- feste Scheduler-/Parameterversion `fsrs-6-java-1.0.0-defaults-v1`
- gewünschte Retention von initial 0,90 pro Benutzer
- reale UTC-Zeitbasis auch für vorgezogene Reviews
- zehnminütige Lern- und Wiederlernschritte, deaktiviertes Fuzzing
- additive Flyway-Migration V5 für aktuellen Zustand und unveränderliche
  Vorher-/Nachher-Snapshots
- weiterhin optimistisches Locking der Review Card und retry-sichere
  Idempotency Keys

## Ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Java-FSRS-Referenz- und Eigenschaftstests | 7 erfolgreich; alle Ratings, deterministische Wiederholung, feste Referenzsequenz, vorgezogener Review und Retention-Monotonie |
| ArchUnit und Spring Modulith | 2 erfolgreich; Domain ohne Framework-/FSRS-Abhängigkeit und Bibliothek auf Scheduling-Adapter begrenzt |
| PostgreSQL-/Security-Integration | Erfolgreich mit PostgreSQL 17.12 über Testcontainers; Flyway V1 bis V5 angewendet |
| Idempotenz und Snapshots | Wiederholter Completion-Request erzeugt exakt einen ReviewLog; Version, Retention und Vorher-/Nachher-Zustand persistiert |
| Gesamter Backend-Testlauf | 14 Tests, 0 Fehler, 0 übersprungen |
| Maven Verify und Checkstyle | Erfolgreich; 0 Checkstyle-Verstöße |
| Container-Build | Erfolgreich mit Java 21; Laufzeitimage enthält die neue Abhängigkeit |
| Erster GitHub-CI-Lauf | [Run 44892223155](https://github.com/milztong/AnkiLeet/actions/runs/44892223155): Backend, Frontend, Terraform und Browser-E2E erfolgreich; Container-Job deckte 14 Java-Funde auf und schlug erwartungsgemäß fehl |
| Dependency-Remediation | Jackson `2.21.7`, Tomcat `10.1.59` und PostgreSQL JDBC `42.7.12`; Dependency Tree und vollständiger Testlauf erfolgreich |
| Container-Security-Scan | CI-identischer lokaler Trivy-Scan `0.76.2`: 0 unbehandelte `HIGH`-/`CRITICAL`-Funde für Alpine und Java; zwei nicht erreichbare Spring-MVC-Funde exakt begrenzt, sichtbar und bis 2026-12-20 befristet |
| Korrigierter GitHub-CI-Lauf | [Run 44914182829](https://github.com/milztong/AnkiLeet/actions/runs/44914182829) für Commit `81bcbb3`: Backend, Frontend, Terraform, Browser-E2E und Container-Build erfolgreich; Backend-Trivy-Scan mit 0 unbehandelten Funden für Alpine und Java |

## Abgleich mit der Definition of Done

- Der Fachkern kennt keine Typen der FSRS-Bibliothek.
- Scheduler-Zeit und Retention werden explizit übergeben; der Adapter liest keine
  Systemzeit und verwendet keine zufällige Intervallstreuung.
- V5 ist additiv und wurde auf einem leeren realen PostgreSQL-Schema ausgeführt.
- Öffentliche REST-Verträge wurden nicht gebrochen; `schedulerVersion` bleibt
  Teil der bestehenden Completion- und History-Antworten.
- Alte `phase3-simple-v1`-Logs bleiben unverändert; bestehende Karten starten
  beim ersten FSRS-Review in `LEARNING`.
- Es wurden keine Cloud-Ressourcen und keine Secrets angelegt.

## Verbleibende Risiken

- Java-FSRS 1.0.0 besitzt keinen Parameter-Optimizer. Personalisierte Parameter
  benötigen später eine eigene, versionierte Entscheidung.
- Das öffentliche Ändern der gewünschten Retention ist bewusst nicht Teil von
  Phase 4; der Wert ist bereits pro Benutzer persistiert.
- Bibliotheks-Upgrades dürfen nur mit neuen Referenzwerten und neuer
  Scheduler-/Parameterversion erfolgen.
- Die zwei Spring-MVC-Ausnahmen müssen bei Einführung von MVC-Views, XSLT,
  SSE oder View-Fragmenten sowie spätestens am 20. Dezember 2026 neu bewertet
  werden. Details stehen in der
  [Backend-Schwachstellenbewertung](../security/backend-vulnerability-review.md).
- Die reproduzierbaren Referenzfälle, Idempotenz, Persistenz und alle
  verbindlichen CI-Gates sind nachgewiesen; das Phase-4-Exit-Kriterium ist
  erfüllt.

## Verbindliche Entscheidungen

- [ADR-0015](../adr/0015-java-fsrs-6-adapter.md)
- [Phase-4-Produktentscheidungen](../product/phase-4-decisions.md)
