variable "name_prefix" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "application_subnet_ids" {
  type = list(string)
}

variable "load_balancer_security_group_id" {
  type = string
}

variable "frontend_security_group_id" {
  type = string
}

variable "backend_security_group_id" {
  type = string
}

variable "backend_repository_url" {
  type = string
}

variable "frontend_repository_url" {
  type = string
}

variable "image_tag" {
  type = string
}

variable "database_endpoint" {
  type = string
}

variable "database_port" {
  type = number
}

variable "database_name" {
  type = string
}

variable "database_secret_arn" {
  type      = string
  sensitive = true
}

variable "kms_key_arn" {
  type = string
}

variable "hosted_zone_id" {
  type = string
}

variable "domain_name" {
  type = string
}

variable "log_retention_days" {
  type = number
}

variable "frontend_cpu" {
  type = number
}

variable "frontend_memory" {
  type = number
}

variable "backend_cpu" {
  type = number
}

variable "backend_memory" {
  type = number
}
