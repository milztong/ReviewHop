# ADR-0011: Zero-Cost Default und kurzlebige AWS-Demos

- Status: Superseded
- Datum: 2026-11-19

> [ADR-0012](0012-hetzner-primary-demo-platform.md) führt Hetzner Cloud als kostengünstige dauerhaft erreichbare Demo ein. AWS bleibt optional und kurzlebig.

## Kontext

Die in ADR-0010 beschriebene Staging-Topologie ist produktionsnah, verursacht aber auch ohne Benutzerverkehr laufende Kosten. Insbesondere ALB, Interface VPC Endpoints, Fargate, RDS und der Customer-Managed KMS Key sind keine dauerhaft kostenlose Umgebung. Für ein Portfolio-Projekt rechtfertigt eine permanent laufende Demo diese Grundlast zunächst nicht.

AWS- und Terraform-Kompetenz sollen weiterhin substanziell und überprüfbar sichtbar sein. Dafür sind reproduzierbare Infrastructure as Code, automatisierte Guardrails, ein sicherer Deploymentpfad und ein belastbarer Rückbau wichtiger als eine ununterbrochen erreichbare URL.

## Entscheidung

ReviewHop verwendet ein **AWS-ready, zero-cost by default** Betriebsmodell:

- Die lokale Docker-Compose-Umgebung ist der dauerhafte Entwicklungs- und Demonstrationsstandard.
- Terraform-Code, Mock-Tests, Linting, Security-Scans und Workflow-Validierung benötigen kein AWS-Konto.
- Kein Push, Pull Request oder Zeitplan führt automatisch einen AWS-`apply` aus.
- Ein reales AWS-Deployment ist optional, kurzlebig und benötigt vor Bootstrap und Staging-Apply jeweils eine ausdrückliche Kostenfreigabe.
- Vor jedem Apply werden Region, aktuelle Pricing-Calculator-Schätzung, Budget, Verantwortlicher, Zweck und geplantes Rückbauzeitfenster dokumentiert.
- Das GitHub-Environment `staging` besitzt Required Reviewers. Der manuelle Workflow verlangt die Bestätigung `DEPLOY-EPHEMERAL` und dokumentiert das geplante Ende des Demo-Fensters.
- Nach einem Demo-Fenster werden zuerst Staging und danach, sofern kein weiteres zeitnahes Deployment beschlossen ist, auch die kostenpflichtigen Bootstrap-Ressourcen kontrolliert zurückgebaut.
- AWS Budgets und Alarme sind Warnsysteme, keine Kostensperre und kein Ersatz für den Rückbau.

ADR-0006 bleibt für AWS als Zielplattform und Terraform als IaC verbindlich. Diese Entscheidung ersetzt jedoch dessen Vorgabe, dass Phase 2 zwingend mit einem realen frühen Deployment endet. ADR-0010 bleibt die Zieltopologie für ein bewusst freigegebenes Demo-Fenster, nicht die dauerhaft laufende Standardumgebung.

## Konsequenzen

Positiv:

- Die normale Projektarbeit erzeugt keine AWS-Kosten.
- Cloud-Architektur, Terraform, OIDC, Security und Cost Governance bleiben als Portfolio-Artefakte sichtbar.
- Ein realer Nachweis kann bei Bedarf reproduzierbar und zeitlich begrenzt erstellt werden.
- Kostenverantwortung und vollständiger Rückbau werden explizit statt implizit behandelt.

Negativ:

- Ohne optionales Demo-Fenster existiert keine dauerhaft öffentlich erreichbare AWS-URL.
- Provider- und Account-spezifische Fehler können erst bei einem freigegebenen echten Plan oder Apply erkannt werden.
- Ein vollständiger Rückbau der selbstverwalteten State- und KMS-Ressourcen benötigt zusätzliche Sorgfalt.
- Screenshots und Deploymentnachweise müssen bei einem kurzlebigen Lauf gezielt erzeugt werden.

## Verworfene Alternativen

### Staging dauerhaft betreiben

Dies liefert eine jederzeit erreichbare URL, erzeugt aber eine laufende Grundlast ohne entsprechenden Produktnutzen.

### AWS vollständig aus dem Projekt entfernen

Dadurch gingen die beabsichtigten Portfolio-Nachweise für Terraform, AWS, IAM/OIDC, Netzwerkgrenzen und Cloudbetrieb verloren.

### Nur auf temporäres Free-Tier-Guthaben vertrauen

Guthaben ist konto-, zeit- und angebotsabhängig. Es ändert weder die regulären Preise noch die Notwendigkeit eines kontrollierten Rückbaus.
