# ADR-0012: Hetzner Cloud als primäre Demo-Plattform

- Status: Accepted
- Datum: 2026-11-19

## Kontext

ReviewHop benötigt für Bewerbungen eine dauerhaft erreichbare Demo. Die bisherige AWS-Zieltopologie aus ECS Fargate, RDS, ALB und mehreren VPC Endpoints ist produktionsnah, verursacht für ein kleines Portfolio-Projekt aber unverhältnismäßige laufende Grundkosten. Ein rein lokaler Betrieb vermeidet diese Kosten, liefert jedoch keine öffentliche Demo.

Die Anwendung ist bereits containerisiert. Clean/Hexagonal Architecture und externe Konfiguration erlauben, denselben Anwendungscode lokal, auf einem virtuellen Server und optional auf AWS zu betreiben.

## Entscheidung

Hetzner Cloud wird die primäre Plattform für die dauerhaft erreichbare Demo:

- ein x86-Cloud-Server in Deutschland als Ausgangspunkt,
- Ubuntu LTS, Docker Engine und Docker Compose,
- Caddy als einziger öffentlicher Anwendungseingang und für automatisches HTTPS,
- Next.js, Spring Boot und PostgreSQL als getrennte Container in einem privaten Compose-Netz,
- keine öffentlichen Ports für Backend oder PostgreSQL,
- eine Hetzner Cloud Firewall für HTTP, HTTPS, ICMP und auf freigegebene CIDRs begrenztes SSH,
- Terraform für Server, Primary IP, SSH Public Key und Firewall,
- serverseitige Backups plus anwendungsbezogene PostgreSQL-Backups mit dokumentiertem Restore,
- GitHub Actions baut unveränderlich getaggte Container-Images und deployt sie nach manueller Freigabe per SSH,
- Secrets liegen weder im Repository, in Container-Images noch im Terraform-State.

Die lokale Docker-Compose-Umgebung bleibt der kostenlose Entwicklungsstandard. AWS wird nicht mehr für Phase 2 oder den öffentlichen Demo-Betrieb vorausgesetzt. Die vorhandene AWS-Infrastruktur bleibt unter `infrastructure/aws` als optionales, kurzlebiges Referenz-Deployment erhalten.

Cloudspezifische Anwendungsdienste sind keine Vorgabe für den Fachkern. OIDC bleibt der Authentifizierungsstandard; der konkrete Produktionsprovider wird vor Phase 3 neu entschieden. Transactional Outbox bleibt verbindlich; der Queue-Transport für Hetzner wird vor Phase 6 neu entschieden. Cognito und SQS bleiben mögliche AWS-Adapter, aber keine Voraussetzung für die Hetzner-Demo.

Diese Entscheidung ersetzt ADR-0006 als primäre Plattformentscheidung sowie das Betriebsmodell aus ADR-0011. ADR-0009 und ADR-0010 bleiben nur als Dokumentation des optionalen AWS-Referenz-Deployments erhalten. Die SQS-Festlegung aus ADR-0007 wird ersetzt; dessen Entscheidung für Transactional Outbox bleibt bestehen. Die Cognito-Festlegung aus ADR-0005 wird ersetzt; dessen Verbot selbst entwickelter Passwortauthentifizierung und die OIDC-Entscheidung bleiben bestehen.

## Konsequenzen

Positiv:

- eine dauerhaft öffentliche Demo ist mit vorhersehbarer kleiner Grundlast möglich,
- lokale und öffentliche Umgebung verwenden dieselben Container und dasselbe PostgreSQL-Schema,
- Terraform, CI/CD, Linux, Docker, TLS, Firewall, Backups und Betrieb werden sichtbar,
- der Fachkern bleibt von Hetzner und AWS unabhängig,
- AWS kann bei Bedarf weiterhin als zusätzlicher Cloud-Nachweis eingesetzt werden.

Negativ:

- Betriebssystem-Patches, Docker, PostgreSQL, Backup und Restore liegen in eigener Verantwortung,
- der einzelne Server ist keine hochverfügbare Produktionsplattform,
- Deployment über SSH benötigt einen sorgfältig geschützten Schlüssel und ein geschütztes GitHub-Environment,
- Skalierung erfolgt zunächst vertikal; horizontale Skalierung benötigt später eine andere Topologie,
- ein Hetzner Cloud API-Token ist für Terraform erforderlich und muss außerhalb des Repositorys verwaltet werden.

## Verworfene Alternativen

### AWS dauerhaft betreiben

Die Managed Services reduzieren Betriebsaufwand und bieten bessere horizontale Skalierung, sind für die erwartete Demo-Last aber zu teuer und komplex.

### Ausschließlich kostenlose PaaS-Angebote

Kostenlose Angebote besitzen wechselnde Laufzeit-, Speicher- und Datenbankgrenzen. Sie sind für eine wegwerfbare Vorschau geeignet, aber keine verlässliche dauerhaft erreichbare Demo.

### Kubernetes auf Hetzner

Ein Cluster würde für die zunächst drei Anwendungscontainer zusätzlichen Betriebs-, Sicherheits- und Kostenaufwand ohne aktuellen Produktnutzen erzeugen.

### Manuell eingerichteter Server ohne Terraform

Dies wäre zwar schnell, aber schlecht reproduzierbar und würde den beabsichtigten Infrastructure-as-Code-Nachweis schwächen.
