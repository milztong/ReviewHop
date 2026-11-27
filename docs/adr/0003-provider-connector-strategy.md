# ADR-0003: Capability-basierte Provider-Connectoren

- Status: Accepted
- Datum: 2026-11-19

## Kontext

Lernplattformen besitzen stark unterschiedliche Integrationsmöglichkeiten: öffentliche Profile, API-Tokens, OAuth2, Webhooks oder gar keine erlaubte automatische Schnittstelle. Ein einheitlicher „Account verbinden“-Ablauf würde Fähigkeiten vortäuschen oder unsichere Sonderfälle erzeugen.

## Entscheidung

Provider implementieren einen `ProviderConnector`-Port und deklarieren Capabilities wie:

- `PUBLIC_PROFILE`
- `OAUTH2`
- `API_TOKEN`
- `MANUAL_IMPORT`
- `INCREMENTAL_SYNC`

Die Benutzeroberfläche leitet mögliche Aktionen aus diesen Capabilities ab.

Version 1 beginnt mit:

1. einem universellen manuellen Provider,
2. Codewars über die dokumentierte öffentliche API.

LeetCode wird zunächst nur als manueller Link-/Metadaten-Provider unterstützt. ReviewHop speichert keine LeetCode-Passwörter oder Session-Cookies und betreibt kein untersagtes Scraping.

## Konsequenzen

Positiv:

- neue Provider verändern den Fachkern nicht,
- Sicherheits- und UX-Eigenschaften sind explizit,
- Providerbeschränkungen werden transparent dargestellt,
- Integrationstests können pro Connector als Vertrag aufgebaut werden.

Negativ:

- nicht jede Plattform bietet denselben Komfort,
- öffentliche Profile bestätigen keine Account-Inhaberschaft,
- Connectoren benötigen individuelle Rate-Limit-, Cursor- und Fehlermodelle.

## Verworfene Alternativen

### Einheitlicher OAuth-Flow für alle Provider

Viele Provider bieten keinen OAuth-Zugang für diesen Anwendungsfall.

### Inoffizielle APIs und Scraping als Standard

Dies ist instabil, sicherheitskritisch und kann Nutzungsbedingungen verletzen.

## Referenzen

- [Codewars API Reference](https://dev.codewars.com/)
- [LeetCode Terms of Service](https://leetcode.com/terms/)
