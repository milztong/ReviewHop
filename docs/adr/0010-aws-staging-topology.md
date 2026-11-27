# ADR-0010: Kostenbewusste AWS-Staging-Topologie

- Status: Deprecated
- Datum: 2026-11-19

> Gilt nur noch für das optionale AWS-Referenz-Deployment. Die primäre Demo-Plattform ist in [ADR-0012](0012-hetzner-primary-demo-platform.md) festgelegt.

## Kontext

Der lokale Walking Skeleton soll produktionsnah auf AWS laufen, ohne bereits die Kosten und Komplexität einer hochverfügbaren Produktionsumgebung zu tragen. Frontend, Backend und PostgreSQL benötigen klare Netzwerkgrenzen. HTTPS, Diagnosefähigkeit und ein kontrollierter Rückbau sind Pflicht.

## Entscheidung

Staging verwendet:

- eine VPC über zwei Availability Zones,
- je zwei öffentliche, private Application- und isolierte Database-Subnets,
- einen öffentlichen Application Load Balancer als einzigen Anwendungseingang,
- ACM und Route 53 für HTTPS; HTTP leitet ausschließlich auf HTTPS um,
- private ECS-Fargate-Tasks ohne öffentliche IP-Adressen,
- AWS Cloud Map für den internen BFF-zu-API-Aufruf,
- eine nicht öffentliche, verschlüsselte RDS-PostgreSQL-17-Instanz,
- ein von RDS in Secrets Manager verwaltetes Master-Credential,
- einen rotierenden Customer-Managed KMS Key für State, RDS, Secrets und SNS,
- CloudWatch Logs, technische Alarme, SNS-E-Mail-Benachrichtigung und AWS Budgets,
- private VPC Endpoints für ECR API/Docker, CloudWatch Logs und Secrets Manager sowie einen S3 Gateway Endpoint.

Security Groups erlauben nur ALB zu Frontend auf Port 3000, Frontend zu Backend auf Port 8080 und Backend zu PostgreSQL auf Port 5432. Das Backend wird nicht über den öffentlichen ALB veröffentlicht.

Für Staging läuft zunächst je eine Frontend- und Backend-Task sowie eine Single-AZ-RDS-Instanz. Die privaten Tasks besitzen keinen allgemeinen Internet-Egress. Neue externe Abläufe müssen ihren Egress in der Phase ergänzen, in der sie tatsächlich eingeführt werden. Single-AZ-RDS ist ein bewusster Kostenkompromiss, keine Produktionsvorlage.

ECS-Deployments verwenden Circuit Breaker und automatisches Rollback. Images werden mit dem unveränderlichen Git-Commit-SHA referenziert. Ein erfolgreicher Apply endet mit einem HTTPS-Smoke-Test der BFF-Route.

## Konsequenzen

Positiv:

- nur der ALB ist öffentlich erreichbar,
- TLS, Datenbankverschlüsselung und Secret-Verwaltung sind von Beginn an enthalten,
- fehlgeschlagene ECS-Deployments rollen zurück,
- derselbe Anwendungscontainer läuft lokal und in AWS,
- Basisalarme und Kostenwarnungen existieren vor dem ersten dauerhaften Betrieb.

Negativ:

- ALB, Interface Endpoints und RDS verursachen auch ohne Benutzerverkehr laufende Kosten,
- Single-AZ-RDS besitzt keine synchrone Standby-Instanz,
- die Anwendung verwendet im Walking Skeleton noch das RDS-Master-Credential für Flyway und Laufzeit; eine getrennte Migrator- und Application-Rolle ist vor öffentlicher Nutzung erforderlich,
- Domain und bestehende öffentliche Route-53-Zone sind Voraussetzungen für den ersten Apply.

## Verworfene Alternativen

### Öffentliche IP-Adressen direkt an ECS-Tasks

Dies wäre günstiger, schwächt aber die sichtbare Netzwerkgrenze und entspricht nicht der vorgesehenen Zielarchitektur.

### NAT Gateways und Multi-AZ-RDS ab Staging-Start

NAT würde allgemeinen Internet-Egress ermöglichen und Multi-AZ-RDS die Datenbankverfügbarkeit erhöhen, aber Angriffsfläche und Grundkosten deutlich steigern. Beides bleibt für konkrete spätere Anforderungen möglich.

### Zweiter interner Load Balancer für das Backend

Cloud Map reicht für den kleinen modularen Monolithen aus und vermeidet einen weiteren dauerhaft kostenpflichtigen Load Balancer.
