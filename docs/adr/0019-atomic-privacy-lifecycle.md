# ADR-0019: Atomarer Privacy-Lebenszyklus über einen dedizierten Port

- Status: Accepted
- Datum: 2026-11-24

## Kontext

PD-009 verlangt einen vollständigen Export und eine vollständige Löschung aller
benutzereigenen ReviewHop-Daten. Diese Daten liegen in den Modulen Identity,
Learning und Integration. Eine teilweise Löschung wäre fachlich und
datenschutzrechtlich falsch; insbesondere dürfen weder Review Logs noch
SyncJobs oder Outbox Events verwaist zurückbleiben.

Die bestehende Modularchitektur verbietet Codezugriffe auf interne Repository-
Adapter anderer Module. Gleichzeitig teilen sich die Module bewusst eine
PostgreSQL-Datenbank und benötigen für die Kontolöschung eine einzige
Transaktionsgrenze.

## Entscheidung

- Ein eigenes Spring-Modul `privacy` verantwortet ausschließlich Export und
  vollständige Löschung der Daten des aktuell authentifizierten `AppUser`.
- Application Use Cases hängen von kleinen Ports für Privacy-Daten und Audit
  ab. Sie kennen weder JDBC noch JPA.
- Ein JDBC-Ausgangsadapter implementiert ein explizites, versioniertes Read
  Model für den Export und löscht den vollständigen Benutzergraphen in genau
  einer PostgreSQL-Transaktion.
- Der Export verwendet einen konsistenten `REPEATABLE READ`-Snapshot. Die
  Löschung sperrt den betroffenen `AppUser`, sodass parallele Schreibvorgänge
  entweder vor der Löschung abgeschlossen werden oder keinen Teilzustand
  erzeugen.
- Der Adapter darf für diesen eng begrenzten Zweck Tabellen mehrerer Module
  lesen und löschen. Er verwendet keine Repository- oder internen Java-Typen
  dieser Module und erzeugt damit keine Codeabhängigkeit zwischen ihnen.
- Neue benutzereigene Tabellen erweitern verpflichtend Export, Löschreihenfolge
  und den PostgreSQL-Integrationstest.
- Geteilte Source Items werden nur gelöscht, wenn nach der Benutzerlöschung
  keine Referenz mehr existiert.
- Der OIDC-Account bleibt außerhalb dieser Transaktion und wird nicht von
  ReviewHop verwaltet.

Diese Entscheidung konkretisiert ADR-0002 für einen fachlich atomaren,
modulübergreifenden Datenschutzvorgang; die übrigen Modulgrenzen bleiben
unverändert.

## Konsequenzen

Positiv:

- keine partiell gelöschten Konten,
- ein nachvollziehbarer Exportvertrag,
- keine zyklischen Modulabhängigkeiten,
- Datenbankverhalten wird gegen echtes PostgreSQL getestet.

Negativ:

- der Privacy-Adapter muss bei jedem neuen benutzereigenen Persistenzmodell
  bewusst aktualisiert werden,
- Tabellenumbenennungen betreffen zusätzlich diesen Adapter,
- die Integrität hängt von einem vollständigen Testinventar ab.

## Verworfene Alternativen

### Jedes Modul löscht unabhängig

Mehrere Transaktionen könnten nach einem Teilfehler einen unvollständigen
Löschzustand hinterlassen. Eine verteilte Kompensation ist für den modularen
Monolithen unnötig komplex.

### Ausschließlich `ON DELETE CASCADE`

Kaskaden vereinfachen die Löschung, bilden aber weder den Export ab noch machen
sie neue benutzereigene Tabellen im Privacy-Vertrag sichtbar. Geteilte
Source Items benötigen außerdem eine referenzabhängige Entscheidung.

### Direkte Repository-Abhängigkeiten zwischen Modulen

Dies würde interne Implementierungen veröffentlichen, Modulzyklen begünstigen
und ADR-0002 verletzen.
