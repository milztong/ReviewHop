locals {
  project               = "ankileet"
  environment           = "staging"
  name_prefix           = "${local.project}-${local.environment}"
  database_name         = "ankileet"
  vpc_dns_resolver_cidr = "${cidrhost(var.vpc_cidr, 2)}/32"

  common_tags = {
    Project     = "AnkiLeet"
    Environment = local.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    CostCenter  = "portfolio"
  }
}
