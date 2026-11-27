# Cloud-Kostenleitplanken

## Primäre Hetzner-Demo

Die öffentliche Demo besitzt eine kleine, aber dauerhafte Grundlast. Kosten entstehen insbesondere durch:

- den gewählten Cloud-Server,
- die Primary IPv4,
- aktivierte Server-Backups,
- optionales externes Backup- oder Object-Storage,
- Domain und DNS beim jeweiligen Registrar oder DNS-Anbieter,
- ausgehenden Datenverkehr oberhalb enthaltener Kontingente.

Konkrete Preise werden unmittelbar vor dem ersten Apply auf der offiziellen Hetzner-Preisseite für Servertyp, Standort, IPv4 und Backups neu geprüft. Preise werden nicht als vermeintlich dauerhafte Zahl im Repository festgeschrieben.

Leitplanken:

- `cx23` in `nbg1` ist nur der kostenbewusste technische Default; der aktuelle Preis benötigt eine Freigabe.
- Backups sind standardmäßig aktiviert und dürfen nur mit dokumentierter alternativer Sicherung deaktiviert werden.
- Das Terraform-Root-Modul erzeugt genau einen Server und keinen Load Balancer oder Kubernetes-Cluster.
- HTTP und HTTPS sind öffentlich; SSH ist auf explizite administrative CIDRs begrenzt.
- Terraform-Plan und monatliche Schätzung werden vor dem ersten Apply gemeinsam geprüft.
- Kostenpflichtige Erweiterungen benötigen eine neue Schätzung und ausdrückliche Freigabe.
- Löschungsschutz verhindert versehentlichen Rückbau, ersetzt aber weder Backups noch Kostenkontrolle.

## Optionales AWS-Referenz-Deployment

Die AWS-Topologie unter `infrastructure/aws` besitzt auch ohne Benutzerverkehr laufende Kosten. Wesentliche Treiber sind ALB, Interface VPC Endpoints, Fargate, RDS, KMS, CloudWatch, Route 53 und Secrets Manager.

AWS bleibt standardmäßig vollständig abgebaut. Ein optionales Demo-Fenster benötigt:

1. aktuelle Pricing-Calculator-Schätzung,
2. absolutes Budget und Alarm-E-Mail,
3. verantwortliche Person,
4. geplantes Rückbauzeitfenster,
5. separate Freigaben für Bootstrap, Staging-Apply und Rückbau.

AWS Budgets warnt, stoppt Ressourcen aber nicht automatisch.

## Vor jedem kostenpflichtigen Apply zu dokumentieren

1. Anbieter, Projekt und Region beziehungsweise Standort,
2. aktueller Preis und enthaltene Leistungen,
3. erwartete monatliche oder zeitlich begrenzte Gesamtkosten,
4. Backup- und Restore-Auswirkungen,
5. verantwortliche Person,
6. Rückbauweg und Folgen für persistente Daten.
