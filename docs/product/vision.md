# Produktvision

## Vision

ReviewHop hilft Lernenden, bereits bearbeitete Aufgaben langfristig zu beherrschen. Die Anwendung verbindet Lernquellen, überführt abgeschlossene Inhalte in aktive Wiederholungen und plant den jeweils nächsten sinnvollen Abruf mit Spaced Repetition.

ReviewHop ist nicht auf Programmieraufgaben beschränkt. Coding-Plattformen bilden den ersten Anwendungsfall, das Domänenmodell unterstützt jedoch auch Übungen, Lektionen, Quizze und manuell erfasste Lerninhalte.

## Problem

Lernplattformen optimieren überwiegend das Entdecken und erstmalige Bearbeiten neuer Inhalte. Bereits gelöste Aufgaben geraten aus dem Blick, obwohl langfristiges Behalten aktive Wiederholung erfordert. Lernende müssen ihre Historie, Notizen und Wiederholungszeitpunkte deshalb häufig manuell verwalten.

Zusätzliche Schwierigkeiten:

- Plattformen bieten unterschiedliche oder keine offiziellen Integrationsmöglichkeiten.
- Eine komplette Programmieraufgabe ist keine klassische kurze Flashcard.
- Wiederholung kann das Erinnern eines Ansatzes oder eine vollständige Neuimplementierung bedeuten.
- Fortschritt, Historie und Wiederholungsqualität sind über Plattformgrenzen verteilt.

## Zielgruppe

Primäre Zielgruppe für Version 1:

- Softwareentwicklerinnen und Softwareentwickler, die Coding-Challenges nachhaltig wiederholen möchten.
- Lernende mit einer vorhandenen Historie auf Codewars oder mit manuell erfassbaren Aufgabenlinks.

Spätere Zielgruppen:

- Lernende auf nicht-technischen Übungsplattformen.
- Personen, die eigene Lernquellen und unterschiedliche Wiederholungsmodi kombinieren möchten.

## Wertversprechen

ReviewHop bietet:

1. eine plattformübergreifende Liste bereits bearbeiteter Lerninhalte,
2. unterschiedliche Wiederholungsmodi für kurze Erinnerung und vollständige Anwendung,
3. nachvollziehbare Planung durch einen austauschbaren Spaced-Repetition-Scheduler,
4. eine unveränderliche Review-Historie,
5. sichere und regelkonforme Plattformverbindungen.

## Kernablauf

1. Ein Benutzer meldet sich an.
2. Er erfasst einen Lerninhalt manuell oder verbindet einen unterstützten Provider.
3. ReviewHop importiert Metadaten zu bereits abgeschlossenen Inhalten.
4. Der Benutzer aktiviert einen Inhalt für die Wiederholung und wählt einen Review-Modus.
5. ReviewHop zeigt fällige Review Cards.
6. Der Benutzer erinnert oder löst den Inhalt und bewertet das Ergebnis.
7. Der Scheduler berechnet den nächsten Termin und protokolliert den Review unveränderlich.

## Version-1-Scope

Version 1 umfasst:

- OIDC-basierte Anmeldung,
- manuell erfasste Lerninhalte,
- automatischen Import abgeschlossener Codewars-Aufgaben,
- die Review-Modi `Approach Recall` und `Full Recode`,
- FSRS-basiertes Scheduling hinter einer austauschbaren Schnittstelle,
- fällige Reviews und Review-Historie,
- eine dauerhaft erreichbare Hetzner-Demo über Terraform und Docker Compose,
- ein optionales kurzlebiges AWS-Referenz-Deployment,
- technische und fachliche Observability.

## Nicht in Version 1

- Ausführen oder Bewerten von Benutzer-Code auf ReviewHop-Infrastruktur,
- automatische LeetCode-Synchronisation durch Scraping oder Session-Cookies,
- soziale Netzwerke, Ranglisten oder öffentliche Profile,
- native Mobilanwendungen,
- KI-generierte Lösungen oder automatische Qualitätsbewertung,
- Abrechnung und kostenpflichtige Pläne,
- Team- oder Organisationskonten.

## Produktprinzipien

- **Aktive Erinnerung vor passivem Lesen:** Reviews fordern eine eigene Antwort, bevor Notizen sichtbar werden.
- **Benutzerkontrolle:** Importierte Inhalte werden nicht automatisch dauerhaft in den Review-Plan aufgenommen.
- **Transparenz:** Der nächste Termin und relevante Scheduler-Zustände sind erklärbar.
- **Provider-Unabhängigkeit:** Fachlogik kennt keine providerspezifischen API-Modelle.
- **Compliance:** Offizielle Schnittstellen und Nutzungsbedingungen begrenzen die Integrationen.
- **Datensparsamkeit:** ReviewHop speichert nur für Lernen und Synchronisation erforderliche Daten.
- **Produktionsnähe:** Security, Tests, Deployment und Observability gehören zum Produkt, nicht zu einer späteren Demonstration.

## Erfolgskriterien für den ersten Release

- Ein neuer Benutzer kann ohne Provider-Verbindung einen Inhalt anlegen und einen vollständigen Review-Zyklus durchführen.
- Ein Codewars-Benutzer kann abgeschlossene Aufgaben ohne Duplikate importieren.
- Wiederholte oder verspätete Requests erzeugen keine doppelten Reviews oder Learning Items.
- Ein technischer Fehler lässt sich über Logs, Metriken und Traces bis zur Ursache verfolgen.
- Die öffentliche Hetzner-Demo lässt sich reproduzierbar per Terraform und CI/CD bereitstellen; dieselben Container laufen lokal und optional in AWS.

Numerische Produktziele, beispielsweise Aktivierungs- oder Wiederholungsraten, werden vor einem öffentlichen Beta-Test festgelegt.
