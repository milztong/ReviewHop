# Definition of Done

Die Kriterien werden proportional zum Arbeitspaket angewendet. Nicht anwendbare Punkte werden im Abschlussbericht als nicht anwendbar benannt; sie werden nicht stillschweigend übersprungen.

## Für jedes Arbeitspaket

- Ziel, Scope und Akzeptanzkriterien sind erfüllt.
- Änderungen sind auf den vereinbarten Scope begrenzt.
- Fachbegriffe stimmen mit dem Glossar überein.
- Clean-Architecture- und Modulgrenzen bleiben erhalten.
- Der Diff enthält keine unbeabsichtigten oder rein kosmetischen Nebenänderungen.
- Relevante Dokumentation und ADRs sind aktuell.
- Ausgeführte Prüfungen und verbleibende Risiken sind dokumentiert.

## Codequalität

- Code ist verständlich benannt und folgt den [Architekturregeln](../adr/0002-clean-hexagonal-architecture.md).
- SOLID wird als Entwurfswerkzeug verwendet, nicht als Grund für spekulative Abstraktionen.
- Keine neuen zyklischen Modul- oder Paketabhängigkeiten.
- Domaincode bleibt frei von Framework- und Infrastrukturabhängigkeiten.
- Fehlerfälle sind explizit modelliert und erzeugen konsistente Antworten.
- Nebenläufigkeit, Transaktionen und Idempotenz wurden für Schreibpfade betrachtet.
- Kein offenes TODO ohne referenzierte Folgeentscheidung oder Aufgabe.

## Tests und Verifikation

- Neue oder geänderte Fachregeln besitzen automatisierte Tests.
- Relevante Unit-, Integrations-, Architektur- und End-to-End-Tests bestehen.
- Datenbankzugriffe werden gegen PostgreSQL getestet, nicht ausschließlich gegen eine In-Memory-Ersatzdatenbank.
- Provider-Adapter testen Erfolg, Pagination, Timeout, Rate Limit und fehlerhafte Antworten.
- Security-Tests prüfen Authentifizierung, Autorisierung und benutzerübergreifenden Objektzugriff.
- Build, Formatierung, statische Analyse und Typprüfung bestehen.
- Testausgaben werden nicht gefälscht oder nur aus früheren Läufen übernommen.

## API und Datenbank

- OpenAPI beschreibt alle geänderten öffentlichen Endpunkte.
- Breaking Changes sind vermieden oder durch eine explizite Entscheidung und Migration abgesichert.
- Eingaben werden an der Systemgrenze validiert.
- Flyway-Migrationen sind vorwärts ausführbar und mit realem PostgreSQL getestet.
- JPA-Entitäten gelangen nicht direkt in öffentliche API-Antworten.
- Datenschutz- und Aufbewahrungsfolgen neuer Felder wurden geprüft.

## Security und Compliance

- Keine Secrets, Tokens, Zugangsdaten oder sensiblen Beispieldaten im Repository.
- Neue Berechtigungen folgen Least Privilege.
- Logs, Traces und Metriken enthalten keine geheimen oder unnötigen personenbezogenen Daten.
- Externe Integrationen verwenden erlaubte und dokumentierte Schnittstellen.
- Abhängigkeiten und Container besitzen keine unbehandelten kritischen Findings.
- Sicherheitsrelevante Rest-Risiken sind sichtbar dokumentiert.

## Cloud und Terraform

- Persistente Ressourcen sind durch Terraform beschrieben.
- `terraform fmt`, `validate`, Linting und ein Review des Plans bestehen.
- Cloud-API-Token, SSH Private Keys und Registry-Zugänge liegen nur in dafür vorgesehenen Secret Stores oder kurzlebigen Prozessumgebungen.
- Netzwerkzugriff, SSH und optionale IAM-Berechtigungen sind minimal erforderlich.
- Ressourcen sind einheitlich getaggt.
- Kostenwirkung und Rückbauweg sind vor `apply` bekannt.
- Ein `apply` oder eine destruktive Änderung wurde ausdrücklich freigegeben.
- Für eine dauerhafte Demo sind Monatskosten, Backup, Patchverantwortung und kontrollierter Rückbau dokumentiert.
- Für ein kurzlebiges optionales Deployment sind Zweck, Kostenschätzung, Verantwortlicher und spätester Rückbauzeitpunkt dokumentiert.

## Observability und Betrieb

- Neue kritische Abläufe propagieren Trace-Kontext.
- Relevante Erfolgs-, Fehler- und Latenzmetriken sind definiert.
- Logs sind strukturiert, korrelierbar und handlungsorientiert.
- Health und Readiness spiegeln den tatsächlichen Dienstzustand wider.
- Fehlerpfade lassen sich diagnostizieren, ohne sensible Daten offenzulegen.
- Betriebs- oder Wiederherstellungsschritte sind dokumentiert, wenn sie sich ändern.

## Review

- Der implementierende Agent hat den eigenen Diff geprüft.
- Für sicherheitskritische, infrastrukturelle oder domänenzentrale Änderungen erfolgt ein separates Review.
- Review-Findings sind behoben, bewusst akzeptiert oder als blockierend dokumentiert.
- Der Benutzer kann Ergebnis, Nachweise und Rest-Risiken ohne Lesen des gesamten Arbeitslogs beurteilen.

## Phase 0

Phase 0 ist abgeschlossen, wenn zusätzlich:

- alle vereinbarten Dokumente existieren,
- interne Links funktionieren,
- zentrale Begriffe eindeutig definiert sind,
- ADRs Status, Kontext, Entscheidung und Konsequenzen enthalten,
- offene Entscheidungen mit spätestem Entscheidungszeitpunkt erfasst sind,
- kein Anwendungscode vorhanden ist.

## Portfolio-Release

Ein öffentlicher Release benötigt zusätzlich:

- reproduzierbares Deployment aus einem sauberen Checkout,
- dokumentiertes lokales Setup,
- erfolgreiche CI/CD-Pipeline,
- getesteten Backup-/Restore-Ablauf,
- Kontolöschung und Datenexport,
- Threat Model und abgeschlossenen Security Review,
- nachvollziehbare Architekturdiagramme,
- Demo-Daten ohne fremde geschützte Inhalte,
- Live-Demo, Screenshots und prägnante Projektbeschreibung.
