locals {
  project     = "ankileet"
  environment = "staging"
  name_prefix = "${local.project}-${local.environment}"

  common_tags = {
    Project     = "AnkiLeet"
    Environment = local.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    CostCenter  = "portfolio"
  }
}
