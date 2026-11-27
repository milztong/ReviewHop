module "observability" {
  source = "../modules/observability"

  name_prefix        = local.name_prefix
  aws_account_id     = data.aws_caller_identity.current.account_id
  alert_email        = var.alert_email
  monthly_budget_usd = var.monthly_budget_usd

  load_balancer_arn_suffix         = module.compute.load_balancer_arn_suffix
  frontend_target_group_arn_suffix = module.compute.frontend_target_group_arn_suffix
  cluster_name                     = module.compute.cluster_name
  frontend_service_name            = module.compute.frontend_service_name
  backend_service_name             = module.compute.backend_service_name
  database_identifier              = module.database.identifier
  kms_key_arn                      = data.aws_kms_alias.platform.target_key_arn
}
