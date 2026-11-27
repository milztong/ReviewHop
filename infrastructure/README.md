# ReviewHop Infrastructure

## Struktur

- `hetzner/`: primäre öffentliche Demo mit Server, Primary IP, SSH Key und Cloud Firewall
- `aws/bootstrap/`: optionaler S3 Remote State, ECR und GitHub-OIDC-Rolle
- `aws/staging/`: optionales AWS-Staging-Root-Modul
- `aws/modules/`: optionale AWS-Module für Netzwerk, Datenbank, Compute und Observability
- `../deploy/hetzner/`: produktionsnahes Docker Compose und Caddy-Konfiguration

Die primäre Entscheidung steht in [ADR-0012](../docs/adr/0012-hetzner-primary-demo-platform.md). Der Aufbau ist im [Hetzner-Demo-Runbook](../docs/operations/hetzner-demo-runbook.md) beschrieben. Die AWS-Unterlagen bleiben als optionales Referenz-Deployment erhalten.

## Lokale Qualitätsprüfungen

Mit installiertem Terraform 1.16:

```powershell
terraform fmt -check -recursive infrastructure
terraform -chdir=infrastructure/hetzner init -backend=false
terraform -chdir=infrastructure/hetzner validate
terraform -chdir=infrastructure/hetzner test
terraform -chdir=infrastructure/aws/bootstrap init -backend=false
terraform -chdir=infrastructure/aws/bootstrap validate
terraform -chdir=infrastructure/aws/staging init -backend=false
terraform -chdir=infrastructure/aws/staging validate
terraform -chdir=infrastructure/aws/staging test
docker compose --file deploy/hetzner/compose.yml --env-file deploy/hetzner/.env.example config --quiet
```

Beide `terraform test`-Suiten verwenden gemockte Provider und erzeugen keine Ressourcen. `terraform plan`, `terraform apply` oder Tests ohne Mock-Provider können dagegen externe Änderungen auslösen und sind nicht Teil der lokalen Standardprüfung.

CI ergänzt TFLint und einen Trivy-Konfigurationsscan, der unbehandelte Findings der Stufen `HIGH` und `CRITICAL` blockiert.
