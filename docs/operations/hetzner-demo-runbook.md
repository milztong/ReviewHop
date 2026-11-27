# Hetzner-Demo-Runbook

Dieses Runbook beschreibt den kontrollierten Aufbau der dauerhaft erreichbaren Demo. Im Rahmen der lokalen Implementierung wurden keine Hetzner-APIs aufgerufen und keine kostenpflichtigen Ressourcen erzeugt.

## Voraussetzungen und Freigaben

Vor dem ersten Apply müssen vorliegen:

- Hetzner-Cloud-Konto und separates Projekt für ReviewHop,
- aktueller Preis für Servertyp, Primary IPv4 und Backups,
- ausdrücklich freigegebenes Monatsbudget,
- Hetzner API Token außerhalb des Repositorys,
- eigenes Ed25519-SSH-Schlüsselpaar ausschließlich für das Deployment,
- administrative Quell-CIDR für SSH,
- öffentliche Domains und Kontrolle über die DNS-A-Records für Anwendung, OIDC und
  die eingeschränkte Keycloak-Administration,
- mindestens eine administrative Quell-CIDR für den Keycloak-Admin-Host,
- GitHub-Repository und geschütztes Environment `hetzner-demo`,
- verantwortliche Person für Patches, Backups, Restore und Kosten.

Der Terraform-State wird zunächst von genau einer verantwortlichen Person lokal in verschlüsseltem Speicher verwaltet und gesichert. Er wird niemals committed oder als Workflow-Artefakt veröffentlicht. Vor Zusammenarbeit mit einer zweiten Person wird ein verschlüsseltes Remote Backend per eigenem ADR festgelegt.

### Budgetentwurf vom 25. November 2026

Der Terraform-Ausgangsplan sieht einen CX23 in Nürnberg (`nbg1`) mit
einer Primary IPv4 und Server-Backups vor. Das verschlüsselte Restic-Ziel
liegt in Falkenstein (`fsn1`). Die Beträge gelten pro vollem Monat.

| Position | Netto | Quelle |
|---|---:|---|
| CX23 in Deutschland | 5,49 € | [Hetzner-Preistabelle](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/) |
| Primary IPv4 | 0,50 € | [IP-Preise](https://docs.hetzner.com/general/infrastructure-and-availability/ipv4-pricing/) |
| Server-Backups, 20 % des Serverpreises | ca. 1,10 € | [Cloud-Abrechnung](https://docs.hetzner.com/cloud/billing/faq/) |
| Object Storage, angesetzter Grundpreis | 4,99 € | [Veröffentlichter Grundpreis](https://www.hetzner.com/pressroom/object-storage/) und [aktuelles Abrechnungsmodell](https://www.hetzner.com/storage/object-storage/) |
| **Geschätzte Grundkosten** | **12,08 €** | **ca. 14,37 € bei 19 % Umsatzsteuer** |

Die Rechnung setzt die enthaltenen Speicher- und Traffic-Kontingente voraus.
Eine neu registrierte Domain und Mehrverbrauch kommen hinzu. Die live in der
Hetzner Console angezeigten Preise sowie die Verfügbarkeit des CX23 müssen vor
dem Apply bestätigt werden. Das Object-Storage-Angebot kann in Abhängigkeit
von Kontingent und Nutzungsdauer weitere Kosten verursachen.

Der Repository-Inhaber hat am 25. November 2026 die geschätzten
Grundkosten von rund 14,37 € brutto pro Monat für CX23, IPv4,
Server-Backups und externen Object Storage freigegeben. Eine neu
registrierte Domain ist separat zu entscheiden. Die Apply-Freigabe
bezieht sich auf den konkreten, geprüften Terraform-Plan.

## 1. Infrastruktur lokal prüfen

```powershell
Copy-Item infrastructure/hetzner/terraform.tfvars.example infrastructure/hetzner/terraform.tfvars
# Platzhalter ersetzen; der API Token gehört nicht in terraform.tfvars.
$env:HCLOUD_TOKEN = "REPLACE_IN_CURRENT_SHELL_ONLY"
terraform -chdir=infrastructure/hetzner init -backend=false
terraform -chdir=infrastructure/hetzner fmt -check
terraform -chdir=infrastructure/hetzner validate
terraform -chdir=infrastructure/hetzner test
terraform -chdir=infrastructure/hetzner plan -out=hetzner.tfplan
```

Den Plan insbesondere auf Serveranzahl, Servertyp, Standort, Backups, Löschungsschutz, Primary IP, SSH-CIDRs und Firewall-Regeln prüfen. Der Token wird nach dem Vorgang aus der Shell entfernt.

## 2. Infrastruktur nach Freigabe anwenden

```powershell
terraform -chdir=infrastructure/hetzner apply hetzner.tfplan
terraform -chdir=infrastructure/hetzner output
Remove-Item Env:HCLOUD_TOKEN
```

Dieser Schritt ist kostenpflichtig und darf nur nach ausdrücklicher Freigabe ausgeführt werden.

## 3. DNS und SSH-Host-Key verifizieren

Die DNS-A-Records für `DEMO_DOMAIN`, `KEYCLOAK_DOMAIN` und
`KEYCLOAK_ADMIN_DOMAIN` zeigen direkt auf den Terraform-Output `ipv4_address`.
Für ReviewHop sind `reviewhop.tongmilz.com`, `auth.reviewhop.tongmilz.com`
und `auth-admin.reviewhop.tongmilz.com` vorgesehen. Vor der Einrichtung
die konkreten Namen in Vercel DNS auf bestehende Einträge prüfen.
Ein vorgeschalteter Proxy würde die CIDR-Prüfung des Admin-Hosts verändern und ist
ohne neue Sicherheitsbewertung nicht Teil dieser Topologie.

Vor dem ersten GitHub-Deployment wird der ED25519-Host-Key-Fingerprint über eine vertrauenswürdige Hetzner-Konsole auf dem Server ausgelesen:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Erst nach Vergleich wird die vollständige `known_hosts`-Zeile als GitHub-Secret gespeichert. Ein ungeprüftes `ssh-keyscan` allein ist kein Vertrauensanker.

## 4. Anwendungssecrets auf dem Server anlegen

Als Benutzer `deploy` wird `/opt/ankileet/.env` einmalig angelegt:

```dotenv
POSTGRES_DB=ankileet
POSTGRES_USER=ankileet_migrator
POSTGRES_PASSWORD=REPLACE_WITH_LONG_RANDOM_MIGRATOR_SECRET
ANKILEET_APP_DB_USERNAME=ankileet_app
ANKILEET_APP_DB_PASSWORD=REPLACE_WITH_DIFFERENT_LONG_RANDOM_APP_SECRET
KEYCLOAK_DB=keycloak
KEYCLOAK_DB_USERNAME=keycloak
KEYCLOAK_DB_PASSWORD=REPLACE_WITH_DIFFERENT_LONG_RANDOM_KEYCLOAK_DB_SECRET
KEYCLOAK_ADMIN_USERNAME=REPLACE_WITH_NON_OBVIOUS_ADMIN_NAME
KEYCLOAK_ADMIN_PASSWORD=REPLACE_WITH_LONG_RANDOM_KEYCLOAK_ADMIN_SECRET
ANKILEET_WEB_CLIENT_SECRET=REPLACE_WITH_LONG_RANDOM_OIDC_CLIENT_SECRET
AUTH_SECRET=REPLACE_WITH_AT_LEAST_32_RANDOM_BYTES
APPLICATION_NETWORK_SUBNET=172.30.0.0/24
CADDY_INTERNAL_IP=172.30.0.10
KEYCLOAK_MEMORY_LIMIT=1536m
```

Danach:

```bash
chmod 600 /opt/ankileet/.env
```

Domain und Image-Tags werden beim Deployment gesetzt. Secrets werden weder in Terraform, GitHub-Variablen, Images noch im Repository gespeichert.

## 5. GitHub-Environment konfigurieren

Environment: `hetzner-demo`

Variablen:

| Variable | Inhalt |
|---|---|
| `HETZNER_HOST` | Terraform-Output `ipv4_address` oder verifizierter Hostname |
| `HETZNER_DEPLOY_USER` | `deploy` |
| `DEMO_DOMAIN` | vollständige öffentliche Domain |
| `KEYCLOAK_DOMAIN` | öffentlicher OIDC-Hostname, beispielsweise `auth.demo.example` |
| `KEYCLOAK_ADMIN_DOMAIN` | separater administrativer Hostname |
| `KEYCLOAK_ADMIN_ALLOWED_CIDRS` | durch Leerzeichen getrennte administrative CIDRs; öffentliche Any-CIDRs sind verboten |

Secrets:

| Secret | Inhalt |
|---|---|
| `HETZNER_SSH_PRIVATE_KEY` | privater Ed25519-Deploymentschlüssel |
| `HETZNER_SSH_KNOWN_HOSTS` | verifizierte Host-Key-Zeile |

Required Reviewers werden aktiviert. Der Hetzner API Token wird nicht für Anwendungsdeployments benötigt und deshalb nicht in GitHub gespeichert.

## 6. Deployment ausführen

Der Workflow `Deploy Hetzner demo` wird manuell auf `main` gestartet. Als Bestätigung muss exakt `DEPLOY-HETZNER` eingegeben werden. Der Workflow:

1. baut Backend-, Frontend- und ein voroptimiertes Keycloak-Image,
2. pusht alle drei Images mit unveränderlichem Commit-SHA nach GHCR,
3. verbindet sich mit geprüftem SSH-Host-Key,
4. aktualisiert Compose-, Caddy-, Realm- und Datenbankinitialisierungskonfiguration,
5. zieht und startet den Release,
6. prüft den BFF über öffentliches HTTPS,
7. entfernt nicht mehr verwendete lokale Images.

Keycloak läuft dabei mit `start --optimized`, eigener PostgreSQL-Datenbank und einem
festen HTTPS-Issuer. Der normale OIDC-Hostname blockiert `/admin` und den `master`-Realm.

OpenAPI und Swagger UI sind im Backend der öffentlichen Demo deaktiviert.
Die lokale Entwicklung behält die API-Dokumentation. Die öffentliche
Anwendungsdomain leitet Anfragen über das Next.js-BFF.
Nur der getrennte Admin-Hostname leitet Anfragen aus den freigegebenen CIDRs weiter.
Port 9000 für Health und Metriken wird nicht durch Caddy veröffentlicht.

`--import-realm` initialisiert ausschließlich eine noch leere Keycloak-Datenbank und
überschreibt keinen vorhandenen Realm. Spätere Realm- oder Clientänderungen benötigen
einen expliziten, versionierten Admin-API-Migrationsschritt. Die produktive
Keycloak-Datenbank darf dafür nicht zurückgesetzt werden. Gleiches gilt für die
Bootstrap-Admin-Variablen: Sie erzeugen nur beim ersten Start den initialen Admin.

## Backup und Restore

Hetzner-Server-Backups bilden die erste Wiederherstellungsebene. Restic ergänzt
sie durch tägliche, clientseitig verschlüsselte logische Dumps der ReviewHop- und
Keycloak-Datenbank in getrenntem Object Storage. Aufbewahrung, Least-Privilege-
Rolle, Timer, RPO/RTO und der ausschließlich isolierte Restore sind im
[Backup- und Restore-Runbook](backup-restore-runbook.md) verbindlich beschrieben.
Ein externer Bucket wird erst nach Kostenfreigabe per Terraform erstellt.

## Diagnose

- TLS fehlt: DNS-A-Record, Ports 80/443 und Caddy-Logs prüfen.
- Deployment scheitert vor SSH: Host-Key-Secret, Quell-CIDR und Cloud Firewall prüfen.
- Container startet nicht: `docker compose ps` und `docker compose logs --since 15m SERVICE` prüfen.
- Datenbank ist nicht bereit: Volume, PostgreSQL-Logs und Werte in `/opt/ankileet/.env` prüfen.
- OIDC-Login scheitert: Keycloak-Readiness, öffentlichen Issuer, DNS und exakte
  Redirect-URI `https://DEMO_DOMAIN/api/auth/callback/keycloak` prüfen.
- Adminzugriff liefert `403`: aktuelle Quell-IP gegen `KEYCLOAK_ADMIN_ALLOWED_CIDRS`
  im geschützten GitHub-Environment prüfen; die Freigabe niemals auf `0.0.0.0/0` oder
  `::/0` erweitern.
- Smoke-Test schlägt fehl: Frontend-, Backend- und Caddy-Health nacheinander prüfen.

## Kontrollierter Rückbau

Vor dem Rückbau werden Backup, Datenaufbewahrung, DNS und Folgen für die öffentliche Demo entschieden. Löschungsschutz wird nur nach ausdrücklicher Freigabe deaktiviert:

```powershell
# deletion_protection in den freigegebenen Variablen bewusst auf false setzen.
terraform -chdir=infrastructure/hetzner plan -out=disable-protection.tfplan
terraform -chdir=infrastructure/hetzner apply disable-protection.tfplan
terraform -chdir=infrastructure/hetzner plan -destroy -out=destroy.tfplan
terraform -chdir=infrastructure/hetzner apply destroy.tfplan
```

Die Primary IP wird durch Terraform mit entfernt. State und gesicherte Backups werden erst nach bestätigter Aufbewahrungsentscheidung gelöscht.
