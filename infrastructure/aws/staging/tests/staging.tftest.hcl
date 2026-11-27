mock_provider "aws" {
  override_during = plan

  mock_data "aws_availability_zones" {
    defaults = {
      names = ["eu-central-1a", "eu-central-1b"]
    }
  }

  mock_data "aws_caller_identity" {
    defaults = {
      account_id = "123456789012"
    }
  }

  mock_data "aws_ecr_repository" {
    defaults = {
      repository_url = "123456789012.dkr.ecr.eu-central-1.amazonaws.com/ankileet"
    }
  }

  mock_data "aws_kms_alias" {
    defaults = {
      target_key_arn = "arn:aws:kms:eu-central-1:123456789012:key/test"
    }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }

  mock_resource "aws_acm_certificate" {
    defaults = {
      arn = "arn:aws:acm:eu-central-1:123456789012:certificate/test"
      domain_validation_options = [{
        domain_name           = "staging.ankileet.example"
        resource_record_name  = "_validation.staging.ankileet.example"
        resource_record_type  = "CNAME"
        resource_record_value = "_validation.acm-validations.aws"
      }]
    }
  }

  mock_resource "aws_db_instance" {
    defaults = {
      address    = "database.ankileet.internal"
      identifier = "ankileet-staging-postgres"
      port       = 5432
      master_user_secret = [{
        kms_key_id    = "alias/aws/secretsmanager"
        secret_arn    = "arn:aws:secretsmanager:eu-central-1:123456789012:secret:ankileet"
        secret_status = "active"
      }]
    }
  }
}

variables {
  aws_region         = "eu-central-1"
  owner              = "ankileet-test"
  image_tag          = "0123456789abcdef"
  domain_name        = "staging.ankileet.example"
  hosted_zone_id     = "Z0123456789EXAMPLE"
  alert_email        = "alerts@ankileet.example"
  monthly_budget_usd = 50
}

run "staging_security_and_cost_baseline" {
  command = plan

  assert {
    condition     = module.compute.https_enabled
    error_message = "The public listener must use HTTPS."
  }

  assert {
    condition     = module.compute.frontend_tasks_private
    error_message = "Frontend tasks must not receive public IP addresses."
  }

  assert {
    condition     = module.compute.backend_tasks_private
    error_message = "Backend tasks must not receive public IP addresses."
  }

  assert {
    condition     = module.database.publicly_accessible == false
    error_message = "PostgreSQL must remain private."
  }

  assert {
    condition     = module.database.storage_encrypted
    error_message = "RDS storage encryption must remain enabled."
  }

  assert {
    condition     = module.database.master_password_managed
    error_message = "RDS must manage its master credential in Secrets Manager."
  }

  assert {
    condition     = module.observability.monthly_budget_usd == 50
    error_message = "The approved monthly budget must reach AWS Budgets."
  }
}
