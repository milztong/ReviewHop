# Phase-2-Verifikationsnachweis

Stand: 19. November 2026

## Ergebnis

Der Hetzner-Walking-Skeleton ist als Terraform-, Cloud-init-, Docker-Compose-, Caddy- und GitHub-Actions-Implementierung vorbereitet und lokal ohne Cloudzugang validiert. Es wurden weder Hetzner- noch AWS-APIs aufgerufen und kein `terraform plan` oder `terraform apply` gegen ein Konto ausgeführt. Es wurden keine kostenpflichtigen Ressourcen erzeugt.

Phase 2 bleibt aktiv, bis Kosten und Betrieb freigegeben, die Hetzner-Demo real bereitgestellt, der öffentliche HTTPS-Smoke-Test bestanden und Backup/Restore nachgewiesen wurden.

## Implementierte primäre Infrastruktur

- Hetzner Cloud Server mit kostenbewusstem x86-Default in Deutschland,
- stabile Primary IPv4,
- Cloud Firewall mit öffentlichen Ports 80/443 und eingeschränktem SSH,
- Cloud-init für Docker, Compose, automatische Security Updates, Fail2ban und einen passwortlosen Deploymentbenutzer,
- Lösch- und Rebuild-Schutz sowie standardmäßig aktivierte Server-Backups,
- Caddy als einziger öffentlicher Eingang mit automatischem HTTPS und Security Headern,
- private Compose-Kommunikation zwischen Next.js, Spring Boot und PostgreSQL,
- kein veröffentlichter Host-Port für Backend oder PostgreSQL,
- unveränderlich per Commit-SHA getaggte GHCR-Images,
- manueller, geschützter GitHub-Workflow mit geprüftem SSH Host Key und HTTPS-Smoke-Test,
- dokumentierte Kosten-, Secret-, Backup-, Restore- und Rückbauleitplanken.

Die bisherige AWS-Infrastruktur wurde nach `infrastructure/aws` verschoben, als optionales Referenz-Deployment gekennzeichnet und erneut validiert. Sie ist kein Phase-2-Abnahmekriterium mehr.

## Ausgeführte Prüfungen

| Bereich | Prüfung | Ergebnis |
|---|---|---|
| Formatierung | Terraform 1.16.7 `fmt -check -recursive` | Erfolgreich |
| Hetzner-Schema | `terraform validate` mit HCloud Provider 1.70.2 | Gültig |
| Hetzner-Guardrails | `terraform test` mit gemocktem HCloud Provider | 1 Test erfolgreich |
| Hetzner-Lint | TFLint 0.64.0 | Keine Findings |
| IaC-Security | Trivy 0.76.2, `HIGH` und `CRITICAL` blockierend | Keine unbehandelten Findings |
| Produktions-Compose | `docker compose config --quiet` | Gültig |
| Caddy | Caddy 2.10.2 `validate` | Gültig |
| Workflows | Actionlint 1.7.12 | Alle Workflows ohne Findings |
| Frontend-Anpassung | ESLint, TypeScript, 5 Vitest-Tests und Linux-Containerbuild | Erfolgreich |
| Optionales AWS-Bootstrap | `terraform validate` mit AWS Provider 6.73.0 | Gültig |
| Optionales AWS-Staging | `terraform validate` und gemocktes `terraform test` | Gültig; 1 Test erfolgreich |
| Optionales AWS-Lint | TFLint mit AWS Ruleset 0.50.0 | Keine Findings |

Trivy erkennt für das Compose-Verzeichnis in dieser Version keinen unterstützten Konfigurationstyp. Deshalb wird die Compose-Datei über Docker Compose selbst validiert; die IaC-Sicherheitsprüfung bezieht sich auf Terraform. Container-Image-Scans bleiben als bestehendes CI-Gate erhalten.

## Vor dem ersten Hetzner-Aufruf erforderlich

1. öffentliches Repository, Lizenz und geschütztes GitHub-Environment `hetzner-demo`,
2. separates Hetzner-Projekt und außerhalb des Repositorys verwalteter API Token,
3. bestätigter Standort, Servertyp, IPv4- und Backup-Preis,
4. ausdrücklich freigegebenes Monatsbudget und Betriebsverantwortlicher,
5. eigenes Ed25519-Deploymentschlüsselpaar und administrative Quell-CIDR,
6. öffentliche Domain und DNS-A-Record,
7. gesicherter lokaler Terraform-State beziehungsweise Entscheidung TD-015,
8. geprüftes Terraform-Plan-Artefakt und ausdrückliche Apply-Freigabe.

## Verbleibende Risiken

- Ein echter Plan kann erst mit Hetzner-Projektkontext Verfügbarkeit, Quotas und Account-spezifische Bedingungen prüfen.
- Der einzelne Server ist ein Single Point of Failure und keine hochverfügbare Produktionsvorlage.
- Betriebssystem, Docker, PostgreSQL und Caddy liegen in eigener Betriebsverantwortung.
- Hetzner-Server-Backups ersetzen kein logisches PostgreSQL-Backup. Ziel,
  Aufbewahrung und automatisierter isolierter Restore sind inzwischen durch
  [ADR-0020](../adr/0020-encrypted-postgresql-backups-with-restic.md) lokal
  umgesetzt; Object-Storage-Apply und realer externer Drill bleiben offen.
- Terraform-State wird zunächst durch genau eine Person lokal verwaltet; ein Remote Backend ist mit TD-015 vor Zusammenarbeit offen.
- Der GitHub-SSH-Deploymentpfad ist statisch validiert, aber erst nach realem Serveraufbau Ende-zu-Ende nachweisbar.
- Der native Next.js-Build unter Windows traf beim parallelen Erzeugen des `.next`-Baums auf einen lokalen `EPERM`-Dateisperrenfehler; derselbe Produktionsbuild im für CI und Hetzner maßgeblichen Linux-Container war erfolgreich.
- Der OIDC-Provider war zum Abschluss dieses Phase-2-Arbeitspakets noch als
  TD-012 offen und wurde inzwischen durch
  [ADR-0013](../adr/0013-keycloak-oidc-and-bff-session.md) entschieden. Der
  damalige Queue-Entscheidungspunkt TD-013 ist inzwischen durch
  [ADR-0017](../adr/0017-postgresql-outbox-worker.md) geschlossen.
- Der Deploymentbenutzer gehört zur Docker-Gruppe und besitzt dadurch faktisch hohe Hostrechte; Schlüssel, Host-Key-Prüfung und Environment-Freigabe sind kritische Kontrollen.
