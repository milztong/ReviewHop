# ADR-0005: OIDC mit Cognito als Produktionsprovider

- Status: Superseded
- Datum: 2026-11-19

> [ADR-0012](0012-hetzner-primary-demo-platform.md) behält OIDC und das Verbot selbst entwickelter Passwortauthentifizierung bei, öffnet aber die Auswahl des Produktionsproviders erneut.

## Kontext

ReviewHop benötigt sichere Benutzeridentitäten, aber keine selbst entwickelte Passwortverwaltung. Gleichzeitig sollen lokale Entwicklung und Tests nicht von einem erreichbaren AWS-Konto abhängen.

## Entscheidung

ReviewHop verwendet OAuth2/OpenID Connect. Das Backend wird als Spring Security OAuth2 Resource Server implementiert.

- AWS Cognito ist der Identity Provider für Staging und Produktion.
- Lokale Entwicklung verwendet einen OIDC-kompatiblen Provider, voraussichtlich Keycloak.
- Der interne Benutzer wird über die Kombination aus vertrauenswürdigem Issuer und Subject referenziert.
- Autorisierung bleibt Aufgabe von ReviewHop und wird nicht allein aus UI-Sichtbarkeit abgeleitet.

## Konsequenzen

Positiv:

- keine Speicherung oder Verarbeitung von Passwörtern,
- standardisierte Tokenprüfung,
- lokale und produktive Umgebungen folgen demselben Protokoll,
- Cognito ergänzt den angestrebten AWS-Stack.

Negativ:

- zwei OIDC-Provider können Konfigurationsunterschiede besitzen,
- Login- und Logout-Flows benötigen End-to-End-Tests,
- Token- und Sessionbehandlung im Browser erfordert eine gesonderte Sicherheitsentscheidung.

## Verworfene Alternativen

### Eigene Benutzername-/Passwortdatenbank

Sie erzeugt unnötiges Sicherheits- und Betriebsrisiko.

### Direkte Bindung des Domainkerns an Cognito

Sie würde lokale Tests und Austauschbarkeit verschlechtern.
