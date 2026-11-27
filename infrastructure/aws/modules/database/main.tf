resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-database"
  subnet_ids = var.database_subnet_ids
}

resource "aws_db_parameter_group" "this" {
  name   = "${var.name_prefix}-postgres17"
  family = "postgres17"

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }
}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-postgres"

  engine         = "postgres"
  engine_version = "17"
  instance_class = var.instance_class

  db_name                       = var.database_name
  username                      = var.master_username
  manage_master_user_password   = true
  master_user_secret_kms_key_id = var.kms_key_arn
  port                          = 5432

  allocated_storage     = var.allocated_storage_gib
  max_allocated_storage = var.allocated_storage_gib * 2
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = var.kms_key_arn

  db_subnet_group_name   = aws_db_subnet_group.this.name
  parameter_group_name   = aws_db_parameter_group.this.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = 1
  backup_window           = "02:00-03:00"
  maintenance_window      = "sun:03:30-sun:04:30"

  auto_minor_version_upgrade      = true
  enabled_cloudwatch_logs_exports = ["postgresql"]
  deletion_protection             = var.deletion_protection
  skip_final_snapshot             = var.skip_final_snapshot
  final_snapshot_identifier       = var.skip_final_snapshot ? null : "${var.name_prefix}-postgres-final"

  apply_immediately = false
}
