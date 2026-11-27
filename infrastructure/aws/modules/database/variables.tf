variable "name_prefix" {
  type = string
}

variable "database_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "database_name" {
  type = string
}

variable "master_username" {
  type = string
}

variable "instance_class" {
  type = string
}

variable "allocated_storage_gib" {
  type = number
}

variable "deletion_protection" {
  type = bool
}

variable "skip_final_snapshot" {
  type = bool
}

variable "kms_key_arn" {
  type = string
}
