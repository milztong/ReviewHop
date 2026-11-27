# ADR-0006: AWS als Zielplattform und Terraform als Infrastructure as Code

- Status: Superseded
- Datum: 2026-11-19

> [ADR-0012](0012-hetzner-primary-demo-platform.md) ersetzt AWS durch Hetzner Cloud als primäre Demo-Plattform. Diese Entscheidung bleibt als Historie des optionalen AWS-Referenz-Deployments erhalten.

## Kontext

Das Projekt soll produktionsnahe Cloud-, Terraform-, Security- und Betriebskenntnisse sichtbar machen. Ein erst am Projektende ergänztes Deployment würde Architektur- und Konfigurationsprobleme spät entdecken.

## Entscheidung

AWS ist die Zielplattform für Staging und die öffentliche Demo. Der lokale Walking Skeleton aus Phase 1 wird bereits in Phase 2 deployt.

Vorgesehene Dienste:

- ECR für Container Images,
- ECS Fargate für API und Worker,
- ALB und ACM für HTTPS,
- RDS PostgreSQL,
- Cognito,
- SQS und Dead-Letter-Queue,
- EventBridge Scheduler,
- KMS und Secrets Manager,
- CloudWatch als AWS-Basistelemetrie.

Alle persistenten Ressourcen werden über Terraform verwaltet. GitHub Actions verwendet AWS-OIDC statt langfristiger Zugangsschlüssel. Ein Apply benötigt Freigabe; Kostenlimit und Region werden vor Provisionierung entschieden.

## Konsequenzen

Positiv:

- Cloud- und Deploymentannahmen werden früh validiert,
- Infrastruktur ist reproduzierbar und reviewbar,
- AWS, Terraform und OIDC sind substanzieller Projektbestandteil,
- Least Privilege und Kostenkontrolle können von Anfang an getestet werden.

Negativ:

- Managed Services verursachen laufende Kosten,
- frühe Cloudarbeit verlangsamt zunächst sichtbare Produktfeatures,
- lokale und AWS-Umgebungen müssen kompatibel gehalten werden.

## Verworfene Alternativen

### AWS erst unmittelbar vor Release

Dies verschiebt Netzwerk-, Security-, Konfigurations- und Deploymentrisiken zu spät.

### EKS als erste Laufzeit

Kubernetes würde für zwei Prozesse zusätzlichen Betriebs- und Kostenaufwand erzeugen. Vorhandene Kubernetes-Kenntnisse werden nicht durch unnötige Clusterkomplexität besser belegt.

### Manuell angelegte AWS-Ressourcen

Sie wären schlecht reproduzierbar, schwer reviewbar und widersprächen dem Terraform-Ziel.
