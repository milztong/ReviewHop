module "database" {
  source = "../modules/database"

  name_prefix           = local.name_prefix
  database_subnet_ids   = module.network.database_subnet_ids
  security_group_id     = aws_security_group.database.id
  database_name         = local.database_name
  master_username       = "ankileet_admin"
  instance_class        = var.database_instance_class
  allocated_storage_gib = var.database_allocated_storage_gib
  deletion_protection   = var.database_deletion_protection
  skip_final_snapshot   = var.database_skip_final_snapshot
  kms_key_arn           = data.aws_kms_alias.platform.target_key_arn
}
