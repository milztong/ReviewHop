# ADR-0009: S3 Remote State und GitHub-OIDC-Deployment

- Status: Deprecated
- Datum: 2026-11-19

> Gilt nur noch für das optionale AWS-Referenz-Deployment. Die primäre Demo-Plattform ist in [ADR-0012](0012-hetzner-primary-demo-platform.md) festgelegt.

## Kontext

Terraform-State darf weder im Repository noch ungeschützt auf einem einzelnen Entwicklergerät liegen. Gleichzeitig soll GitHub Actions ohne langfristige AWS-Zugangsschlüssel deployen. Die ECR-Repositories müssen existieren, bevor der erste Anwendungs-Workflow Images pushen und ECS darauf verweisen kann.

## Entscheidung

Ein getrenntes Bootstrap-Root-Modul erzeugt:

- einen privaten, verschlüsselten und versionierten S3-Bucket für Terraform-State,
- S3-native State-Lockfiles mit `use_lockfile = true`,
- immutable ECR-Repositories für Backend und Frontend,
- den GitHub-OIDC-Provider,
- eine Deployment-Rolle für genau einen OIDC-Subject-Claim des GitHub-Environments `staging`.

Die erste Bootstrap-Ausführung verwendet lokalen State. Direkt danach wird dieser State in denselben S3-Bucket migriert. Das Staging-Root-Modul verwendet von Beginn an einen partiell konfigurierten S3-Backend-Block. Bucket und Region werden beim `terraform init` injiziert und nicht in Plan-Dateien oder Quellcode dupliziert.

DynamoDB wird nicht für State-Locking verwendet, da Terraform S3-native Lockfiles unterstützt und DynamoDB-basiertes Locking als veraltet dokumentiert ist.

GitHub Actions erhält `id-token: write` nur im manuellen Deployment-Workflow. Die IAM-Trust-Policy prüft sowohl `aud = sts.amazonaws.com` als auch den exakten `sub`-Claim. Das GitHub-Environment `staging` muss mit Required Reviewers geschützt werden.

## Konsequenzen

Positiv:

- kein langfristiger AWS-Schlüssel in GitHub,
- State-Wiederherstellung über S3-Versionen,
- serialisierte State-Änderungen ohne zusätzliche DynamoDB-Ressource,
- Images sind vor dem ersten Staging-Plan verfügbar,
- OIDC-Trust ist auf Repository und Environment begrenzt.

Negativ:

- der einmalige Bootstrap benötigt eine bereits autorisierte administrative AWS-Identität,
- der erste lokale Bootstrap-State muss unmittelbar migriert und anschließend sicher entfernt werden,
- eine Umbenennung von Repository oder Environment erfordert eine Trust-Policy-Änderung,
- die Deployment-Rolle benötigt für Terraform breitere Rechte als die späteren ECS-Task-Rollen.

## Verworfene Alternativen

### Langfristige IAM-User-Schlüssel in GitHub Secrets

Diese Schlüssel müssten rotiert werden und vergrößern die Auswirkung eines Secret-Leaks.

### Lokaler State oder State im Repository

Beides bietet weder belastbare Zusammenarbeit noch angemessenen Schutz vor Verlust, Parallelzugriff oder versehentlicher Veröffentlichung.

### DynamoDB-Lock-Tabelle

Sie erzeugt eine zusätzliche Ressource für einen inzwischen veralteten Locking-Pfad.
