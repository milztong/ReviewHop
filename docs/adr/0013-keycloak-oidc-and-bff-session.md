# ADR-0013: Keycloak als OIDC-Provider und serverseitige BFF-Session

- Status: Accepted
- Datum: 2026-11-20

## Kontext

Phase 3 benötigt einen OIDC-Provider für lokale Entwicklung und die Hetzner-Demo. Ein externer Managed Provider reduziert den Betriebsaufwand, führt für das Portfolio-Projekt jedoch eine weitere Kosten-, Konto- und Datenschutzabhängigkeit ein. Keycloak ist OIDC-kompatibel, lokal reproduzierbar und kann auf derselben Hetzner-Plattform betrieben werden. ReviewHop darf trotzdem weder Passwörter noch Provider-Sessions selbst verwalten.

Der Browser soll keine Access- oder Refresh-Tokens in Local Storage oder clientseitig lesbaren Sessions erhalten. ADR-0008 legt Next.js bereits als BFF fest.

## Entscheidung

- Keycloak ist der OIDC-Provider für lokale Entwicklung und die primäre Hetzner-Demo.
- Spring Boot validiert Bearer-JWTs als OAuth2 Resource Server. Der interne `AppUser` wird ausschließlich über die Kombination aus exaktem Issuer und Subject zugeordnet.
- Auth.js führt im Next.js-BFF den Authorization Code Flow aus. Das Access-Token liegt nur im verschlüsselten, sicheren HttpOnly-Session-Cookie und wird serverseitig an eng begrenzte Java-Endpunkte weitergereicht. Es wird nicht in die für Browser-JavaScript sichtbare Session kopiert.
- Die initiale BFF-Session und das Keycloak-Access-Token laufen nach einer Stunde ab. Phase 3 fordert dann eine neue Anmeldung; es wird zunächst kein langlebiges Refresh-Token gespeichert.
- Der lokale Realm darf Selbstregistrierung anbieten, enthält aber keine Testpasswörter oder festen Secrets. Admin-, Client- und Session-Secrets kommen ausschließlich aus der Umgebung.
- Der lokale Container verwendet `start-dev`; die öffentliche Hetzner-Instanz muss `start` mit externer PostgreSQL-Datenbank, festem HTTPS-Hostname, Health Checks und begrenztem Adminzugriff verwenden. Diese Produktionskonfiguration ist ein eigenes Phase-3-Arbeitspaket und noch keine Apply-Freigabe.
- Der aktuelle Keycloak-Container wird bis zu einem vom Anbieter bestätigten Runtime-Fix mit `JAVA_OPTS_APPEND=-XX:-UseBASE64Intrinsics` gestartet. Der laufende JVM-Flag muss vor einer öffentlichen Bereitstellung verifiziert werden. Grundlage ist der [offizielle Keycloak-Sicherheitshinweis zu CVE-2026-114938](https://www.keycloak.org/2026/11/java4shell-vulnerability-cve-2026-114938).
- Der Fachkern und die API-Verträge bleiben OIDC-providerneutral. Das optionale AWS-Deployment darf Cognito als kompatiblen Adapter verwenden.

Diese Entscheidung konkretisiert ADR-0008 und ersetzt die offene Providerauswahl aus ADR-0012 beziehungsweise TD-012. ADR-0005 bleibt superseded.

## Konsequenzen

Positiv:

- identischer OIDC-Standard lokal und auf Hetzner,
- keine selbst entwickelte Passwortauthentifizierung,
- Access-Tokens sind nicht durch Browser-JavaScript lesbar,
- der Backend-Fachkern hängt weder von Keycloak noch von Auth.js ab,
- Login und Benutzergrenzen können lokal reproduzierbar getestet werden.

Negativ:

- Keycloak erhöht RAM-, Patch-, Backup- und Monitoringbedarf der Hetzner-Demo,
- Realm- und Keycloak-Daten benötigen einen eigenen Restore-Nachweis,
- eine abgelaufene einstündige Sitzung verlangt zunächst eine erneute Anmeldung,
- das BFF ist eine zusätzliche sicherheitskritische Komponente,
- der aktuelle Java-Runtime-Hinweis verlangt eine explizite Mitigation und Verifikation.

## Verworfene Alternativen

### Eigene Benutzername-/Passwortauthentifizierung

Sie würde unnötig Credential-Speicherung, Account-Recovery und zusätzliche Angriffsfläche erzeugen.

### Access-Token im Browser oder Local Storage

Dies würde den Nutzen des BFF aufheben und Token bei XSS direkt lesbar machen.

### Externer Managed Provider als primärer Demo-Provider

Dies bleibt eine spätere Betriebsalternative, ist aber für den aktuellen lokalen und kostenkontrollierten Demo-Pfad nicht erforderlich.
