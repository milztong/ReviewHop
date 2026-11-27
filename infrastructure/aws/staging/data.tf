data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"

  filter {
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }
}

data "aws_ecr_repository" "backend" {
  name = "${local.name_prefix}-backend"
}

data "aws_ecr_repository" "frontend" {
  name = "${local.name_prefix}-frontend"
}

data "aws_kms_alias" "platform" {
  name = "alias/${local.name_prefix}"
}
