# Optionales AWS-Staging-Runbook

Dieses Runbook beschreibt das optionale, kurzlebige AWS-Referenz-Deployment. Die primäre öffentliche Demo läuft gemäß ADR-0012 auf Hetzner. Kein Befehl wurde im Rahmen der lokalen Phase-2-Implementierung gegen ein AWS-Konto ausgeführt.

## Voraussetzungen und Freigaben

Vor dem ersten Bootstrap müssen entschieden oder vorhanden sein:

- AWS-Konto und Region,
- monatliches Kostenlimit und Alarm-E-Mail,
- öffentliche Domain und Route-53-Hosted-Zone,
- GitHub-Repository und geschütztes Environment `staging`,
- exakter OIDC-`sub`-Claim dieses Environments,
- ausdrückliche Freigabe für kostenpflichtige AWS-Ressourcen.

Die einmalige administrative Anmeldung soll über AWS IAM Identity Center oder eine gleichwertige kurzlebige Identität erfolgen, nicht über neu erzeugte langfristige Access Keys.

## 1. Bootstrap prüfen

```powershell
Set-Location infrastructure/aws/bootstrap
Copy-Item terraform.tfvars.example terraform.tfvars
# Platzhalter in terraform.tfvars ersetzen.
terraform init -backend=false
terraform fmt -check
terraform validate
terraform plan -out=bootstrap.tfplan
```

Den Plan insbesondere auf Account-ID, Region, Bucket, ECR-Namen, OIDC-Subject und IAM-Rechte prüfen.

## 2. Bootstrap nach Freigabe anwenden

```powershell
terraform apply bootstrap.tfplan
terraform output
```

Dieser Schritt erzeugt bereits persistente AWS-Ressourcen und benötigt deshalb eine ausdrückliche Freigabe.

## 3. Bootstrap-State nach S3 migrieren

```powershell
terraform init -migrate-state `
  -backend-config="bucket=REPLACE_STATE_BUCKET" `
  -backend-config="region=REPLACE_AWS_REGION"
```

Nach erfolgreicher Migration prüfen, dass `terraform state list` funktioniert und kein lokaler `terraform.tfstate` mehr benötigt wird. State-Dateien dürfen niemals committed werden.

## 4. GitHub-Environment konfigurieren

Environment: `staging`

Erforderliche Repository-/Environment-Variablen:

| Variable | Inhalt |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | Bootstrap-Output `github_deploy_role_arn` |
| `AWS_REGION` | freigegebene Region |
| `TF_STATE_BUCKET` | Bootstrap-Output `state_bucket_name` |
| `OWNER` | technischer Verantwortlicher |
| `STAGING_DOMAIN_NAME` | vollständiger Staging-Hostname |
| `STAGING_HOSTED_ZONE_ID` | bestehende öffentliche Route-53-Zone |
| `ALERT_EMAIL` | Empfänger für Budget und Alarme |
| `MONTHLY_BUDGET_USD` | ausdrücklich freigegebenes Monatslimit |

Für das Environment werden Required Reviewers aktiviert. Es werden keine `AWS_ACCESS_KEY_ID`- oder `AWS_SECRET_ACCESS_KEY`-Secrets angelegt.

## 5. Deployment ausführen

Der Workflow `Deploy optional AWS staging` wird manuell auf dem Branch `main` gestartet. Als Bestätigung muss exakt `DEPLOY` eingegeben werden. Nach Environment-Freigabe:

1. bezieht GitHub ein kurzlebiges OIDC-Token,
2. baut und pusht beide Images mit dem Commit-SHA,
3. erzeugt einen Terraform-Plan,
4. wendet genau diesen Plan an,
5. prüft `/api/system-status` über HTTPS.

Die SNS-E-Mail-Subscription muss nach dem ersten Apply über die Bestätigungs-E-Mail aktiviert werden.

## Diagnose

- Fehlgeschlagene Task-Starts: ECS-Service-Events und `/ankileet/staging/*`-Log-Groups prüfen.
- Fehlgeschlagene Datenbankverbindung: RDS-Status, Backend-Security-Group und Secret-Zugriff der Execution Role prüfen.
- Zertifikat bleibt `Pending validation`: Route-53-Validierungsrecord und Hosted-Zone prüfen.
- OIDC schlägt fehl: tatsächlichen GitHub-`sub`-Claim mit `github_oidc_subject` vergleichen; Trust nicht per Wildcard auf weitere Repositories öffnen.
- Smoke-Test schlägt fehl: ALB-Target-Health, ECS Circuit Breaker und BFF-Antwort prüfen.

## Rückbau

Der Rückbau ist ebenfalls kosten- und datenrelevant und benötigt Freigabe:

```powershell
Set-Location infrastructure/aws/staging
terraform plan -destroy -out=destroy.tfplan
terraform apply destroy.tfplan
```

Vorher entscheiden, ob ein RDS-Final-Snapshot benötigt wird. Bootstrap-Ressourcen werden erst entfernt, wenn kein Staging-State und kein benötigtes Image mehr existiert. Der State-Bucket besitzt `prevent_destroy` und wird nicht beiläufig gelöscht.
