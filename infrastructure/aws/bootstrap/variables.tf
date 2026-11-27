variable "aws_region" {
  description = "AWS region used for bootstrap resources."
  type        = string
  default     = "eu-central-1"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for Terraform state."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$", var.state_bucket_name))
    error_message = "state_bucket_name must be a valid, globally unique S3 bucket name."
  }
}

variable "github_oidc_subject" {
  description = "Exact GitHub OIDC sub claim allowed to assume the deployment role."
  type        = string

  validation {
    condition     = startswith(var.github_oidc_subject, "repo:") && strcontains(var.github_oidc_subject, ":environment:staging")
    error_message = "github_oidc_subject must target one repository and the staging environment."
  }
}

variable "permissions_boundary_arn" {
  description = "Optional IAM permissions boundary attached to roles created by the bootstrap."
  type        = string
  default     = null
  nullable    = true
}

variable "owner" {
  description = "Technical owner tag, for example a GitHub handle."
  type        = string
}
