# ADR-0008: Next.js als Node-Server und Backend for Frontend

- Status: Accepted
- Datum: 2026-11-19

> Die BFF-Entscheidung bleibt gültig. [ADR-0012](0012-hetzner-primary-demo-platform.md) ersetzt Cognito und ECS als feste Produktionsvorgaben durch einen austauschbaren OIDC-Provider und die primäre Hetzner-Demo.

## Kontext

Das Frontend benötigt zunächst nur eine Verbindung zur Java-API. In Phase 3 kommt OIDC mit Cognito hinzu. Eine rein statische Single-Page-Anwendung müsste Access- und Refresh-Tokens selbst im Browser verwalten oder später grundlegend umgebaut werden.

## Entscheidung

Next.js wird als Node.js-Server im Standalone-Modus betrieben. Browseranfragen an die Java-API laufen über eng begrenzte Next.js Route Handler. Diese Schicht bildet das Backend for Frontend (BFF).

Im Walking Skeleton ruft `/api/system-status` den Java-Endpunkt `/api/v1/system/status` serverseitig auf. In Phase 3 übernimmt der BFF den OIDC Authorization Code Flow und schützt die Browser-Session mit sicheren HttpOnly-Cookies. Die Java-API bleibt ein OAuth2 Resource Server.

## Konsequenzen

Positiv:

- Tokens müssen nicht in Local Storage gespeichert werden,
- Browser und Java-API benötigen keine direkte CORS-Kopplung,
- interne API-Adressen bleiben serverseitige Konfiguration,
- Next.js kann auf ECS als standardisierter Container betrieben werden.

Negativ:

- das Frontend benötigt einen laufenden Node.js-Prozess,
- ein zusätzlicher Netzwerk-Hop entsteht,
- BFF-Endpunkte benötigen eigene Timeout-, Fehler- und Sicherheitstests,
- horizontale Skalierung muss später Session- und Cacheeigenschaften berücksichtigen.

## Verworfene Alternativen

### Statischer Export mit direktem Browserzugriff auf die Java-API

Dies wäre für Phase 1 kleiner, verschiebt aber sichere OIDC-Sessionbehandlung und CORS in den Browser.

### Fachlogik im BFF

Der BFF übersetzt Browser- und Authentifizierungsbelange. Fachliche Regeln bleiben ausschließlich in der Java-Anwendung.
