output "state_bucket_name" {
  description = "S3 bucket used for Terraform state."
  value       = aws_s3_bucket.terraform_state.id
}

output "github_deploy_role_arn" {
  description = "Role ARN configured as the AWS_DEPLOY_ROLE_ARN GitHub environment variable."
  value       = aws_iam_role.github_deploy.arn
}

output "ecr_repository_urls" {
  description = "Application ECR repository URLs."
  value       = { for name, repository in aws_ecr_repository.application : name => repository.repository_url }
}

output "platform_kms_key_arn" {
  description = "Customer-managed key used by staging platform resources."
  value       = aws_kms_key.platform.arn
}
