module "compute" {
  source = "../modules/compute"

  name_prefix            = local.name_prefix
  aws_region             = var.aws_region
  vpc_id                 = module.network.vpc_id
  public_subnet_ids      = module.network.public_subnet_ids
  application_subnet_ids = module.network.application_subnet_ids

  load_balancer_security_group_id = aws_security_group.load_balancer.id
  frontend_security_group_id      = aws_security_group.frontend.id
  backend_security_group_id       = aws_security_group.backend.id

  backend_repository_url  = data.aws_ecr_repository.backend.repository_url
  frontend_repository_url = data.aws_ecr_repository.frontend.repository_url
  image_tag               = var.image_tag

  database_endpoint   = module.database.endpoint
  database_port       = module.database.port
  database_name       = local.database_name
  database_secret_arn = module.database.master_secret_arn
  kms_key_arn         = data.aws_kms_alias.platform.target_key_arn

  hosted_zone_id = var.hosted_zone_id
  domain_name    = var.domain_name

  log_retention_days = var.log_retention_days
  frontend_cpu       = 256
  frontend_memory    = 512
  backend_cpu        = 512
  backend_memory     = 1024
}
