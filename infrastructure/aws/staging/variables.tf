variable "aws_region" {
  description = "AWS region for the staging environment."
  type        = string
  default     = "eu-central-1"
}

variable "owner" {
  description = "Technical owner tag, for example a GitHub handle."
  type        = string
}

variable "image_tag" {
  description = "Immutable backend and frontend image tag, normally the Git commit SHA."
  type        = string

  validation {
    condition     = length(var.image_tag) >= 7 && can(regex("^[A-Za-z0-9._-]+$", var.image_tag))
    error_message = "image_tag must be an immutable, registry-safe identifier."
  }
}

variable "domain_name" {
  description = "Fully qualified staging hostname, for example staging.ankileet.example."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$", var.domain_name))
    error_message = "domain_name must be a valid lowercase DNS hostname."
  }
}

variable "hosted_zone_id" {
  description = "Existing Route 53 public hosted zone ID containing domain_name."
  type        = string
}

variable "alert_email" {
  description = "Recipient for budget and operational alarms."
  type        = string
}

variable "monthly_budget_usd" {
  description = "Approved monthly AWS budget in USD."
  type        = number
}

variable "vpc_cidr" {
  type    = string
  default = "10.42.0.0/16"
}

variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.42.0.0/24", "10.42.1.0/24"]
}

variable "application_subnet_cidrs" {
  type    = list(string)
  default = ["10.42.10.0/24", "10.42.11.0/24"]
}

variable "database_subnet_cidrs" {
  type    = list(string)
  default = ["10.42.20.0/24", "10.42.21.0/24"]
}

variable "database_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "database_allocated_storage_gib" {
  type    = number
  default = 20
}

variable "database_deletion_protection" {
  description = "Enable after the first stable deployment if the staging data must survive accidental destroy."
  type        = bool
  default     = false
}

variable "database_skip_final_snapshot" {
  description = "Cost-conscious staging default; set to false before retaining important data."
  type        = bool
  default     = true
}

variable "log_retention_days" {
  type    = number
  default = 30
}
