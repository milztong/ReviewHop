variable "name_prefix" {
  type = string
}

variable "aws_account_id" {
  type = string
}

variable "alert_email" {
  type = string

  validation {
    condition     = can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", var.alert_email))
    error_message = "alert_email must be a syntactically valid email address."
  }
}

variable "monthly_budget_usd" {
  type = number

  validation {
    condition     = var.monthly_budget_usd > 0
    error_message = "monthly_budget_usd must be greater than zero."
  }
}

variable "load_balancer_arn_suffix" {
  type = string
}

variable "frontend_target_group_arn_suffix" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "frontend_service_name" {
  type = string
}

variable "backend_service_name" {
  type = string
}

variable "database_identifier" {
  type = string
}

variable "kms_key_arn" {
  type = string
}
