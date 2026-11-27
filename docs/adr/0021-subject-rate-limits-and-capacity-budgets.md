# ADR-0021: Subjektbezogene API-Limits und feste Kapazitätsbudgets

- Status: Accepted
- Datum: 2026-11-24

## Kontext

Die öffentliche Portfolio-Demo läuft auf einem einzelnen kostenbewussten
Hetzner-Server. Authentifizierte API-Aufrufe erreichen Spring Boot über das
Next.js-BFF. Ohne messbares Kapazitätsziel und Begrenzung könnte ein einzelner
angemeldeter Benutzer Datenbankpool, CPU oder schreibintensive Pfade unnötig
belegen. IP-basierte Limits wären hinter dem BFF ungenau, weil mehrere Benutzer
aus Sicht des Backends denselben BFF-Ursprung teilen.

Die vorhandenen Idempotenz-, Outbox- und Provider-Retry-Verträge schützen die
Korrektheit, ersetzen aber weder Lastbudgets noch eine faire Zuteilung der
API-Kapazität.

## Entscheidung

- Das Backend begrenzt authentifizierte `/api/v1`-Aufrufe pro Kombination aus
  OIDC-Issuer und Subject. Fremde Provider-Identitäten können dadurch nicht
  denselben Zähler teilen.
- Sichere Lesezugriffe erhalten 300 Requests pro Minute, Schreibzugriffe 60
  Requests pro Minute. Der öffentliche Systemstatus bleibt ausgenommen;
  Keycloak schützt Anmeldung und Registrierung mit seinen eigenen
  Brute-Force-Kontrollen.
- Ein kleiner Infrastrukturfilter nach der Bearer-Token-Authentifizierung
  implementiert feste Zeitfenster. Er kennt weder Fachobjekte noch Use Cases.
  Überschreitungen liefern HTTP 429 als Problem Detail sowie `Retry-After`,
  `X-RateLimit-Limit` und `X-RateLimit-Remaining`.
- Zähler liegen für die Ein-Instanz-Demo lokal im Prozess. Ein größenbegrenzter
  Cache hält höchstens 10.000 Identitäten mit getrennten Lese-/Schreibzählern;
  inaktive Einträge verfallen nach zwei Fenstern.
- Die technische Metrik `ankileet.security.rate_limit.requests` unterscheidet
  Lese-/Schreibzugriff und `allowed`/`rejected`, enthält aber weder Subject noch
  Route.
- Der Release-CI-Lauf prüft über echte HTTP-, OIDC- und PostgreSQL-Pfade:

  | Pfad | Lastbudget | Latenzbudget |
  |---|---:|---:|
  | Due-Liste | 5 Requests/Sekunde für 15 Sekunden | p95 unter 500 ms |
  | Learning Item anlegen und Review abschließen | 1 vollständiger Zyklus/Sekunde für 15 Sekunden | p95 unter 1.500 ms |

  Beide Szenarien erlauben weniger als 1 % fachliche Fehler und keine
  verworfenen Iterationen.
- Ein PostgreSQL-Integrationstest sendet jeweils acht gleichzeitige identische
  Erstellungs- und Completion-Requests. Alle Antworten müssen auf genau ein
  `LearningItem`, eine `ReviewCard` und ein `ReviewLog` zeigen.
- Provider-Ausfälle bleiben von Benutzerrequests entkoppelt. Connect-Timeout
  zwei Sekunden, Read-Timeout fünf Sekunden, begrenzte Retries mit Jitter und
  Dead Letter bilden den Resilienzvertrag.

## Konsequenzen

- Das Limit ist auf die aktuelle Ein-Instanz-Topologie zugeschnitten und wird
  bei mehreren Backend-Replikaten nicht global konsistent. Eine Skalierung auf
  mehrere Instanzen benötigt einen verteilten Zähleradapter oder ein Edge-Limit
  und ein neues ADR.
- Feste Fenster sind leicht erklärbar und betrieblich günstig, erlauben aber
  einen Burst an einer Fenstergrenze. Die Budgets sind Schutzgrenzen, keine
  zugesicherte Service-Rate.
- Eine Cache-Eviction kann einer Identität vorzeitig ein neues Kontingent
  geben. Das Größenlimit ist für die Portfolio-Demo bewusst deutlich oberhalb
  der erwarteten Benutzerzahl und verhindert gleichzeitig ungebundenes
  Speicherwachstum.
- Der k6-Lauf ist ein reproduzierbares Release-Gate, keine Aussage über die
  maximale Kapazität eines später ausgewählten Hetzner-Servertyps. Nach dem
  echten Deployment wird derselbe Test erneut gegen die interne Demo-Topologie
  ausgeführt, bevor Limits erhöht werden.

## Verworfene Alternativen

- **Nur IP-basiertes Limit in Spring Boot:** hinter dem BFF keine faire
  Benutzertrennung und fehleranfällige Proxy-Header-Auswertung.
- **Caddy-Rate-Limit-Plugin:** benötigt ein eigenes Caddy-Build und besitzt ohne
  Tokenauswertung dieselbe BFF-Ungenauigkeit.
- **Redis als verteilter Zähler:** zusätzlicher zustandsbehafteter Dienst ohne
  aktuellen Mehrinstanz-Anwendungsfall.
- **Kein Limit, nur Monitoring:** erkennt Überlast erst nach ihrem Eintritt und
  schließt SR-06 nicht.

## Verweise

- [ADR-0008](0008-nextjs-bff.md)
- [ADR-0017](0017-postgresql-outbox-worker.md)
- [Kapazitäts- und Resilienz-Runbook](../operations/capacity-resilience-runbook.md)
