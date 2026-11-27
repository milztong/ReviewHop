output "application_url" {
  description = "HTTPS URL used by the deployment smoke test."
  value       = module.compute.application_url
}

output "backend_repository_url" {
  value = data.aws_ecr_repository.backend.repository_url
}

output "frontend_repository_url" {
  value = data.aws_ecr_repository.frontend.repository_url
}

output "database_secret_arn" {
  description = "ARN only; the secret value is never a Terraform output."
  value       = module.database.master_secret_arn
  sensitive   = true
}

output "alarm_topic_arn" {
  value = module.observability.alarm_topic_arn
}
