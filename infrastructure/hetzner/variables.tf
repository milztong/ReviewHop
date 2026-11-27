variable "owner" {
  description = "Technical owner used for resource labels."
  type        = string

  validation {
    condition     = length(trimspace(var.owner)) >= 2
    error_message = "owner must identify a responsible person or team."
  }
}

variable "location" {
  description = "Hetzner Cloud location for the demo server."
  type        = string
  default     = "nbg1"

  validation {
    condition     = contains(["fsn1", "nbg1"], var.location)
    error_message = "The primary demo must remain in a German Hetzner location (fsn1 or nbg1)."
  }
}

variable "server_type" {
  description = "Hetzner x86 shared-vCPU server type; confirm current pricing before apply."
  type        = string
  default     = "cx23"

  validation {
    condition     = startswith(var.server_type, "cx") || startswith(var.server_type, "cpx")
    error_message = "Use an x86 CX or CPX server type unless an architecture change is documented."
  }
}

variable "server_image" {
  description = "Ubuntu LTS image used for the Docker host."
  type        = string
  default     = "ubuntu-24.04"
}

variable "ssh_public_key" {
  description = "Public SSH key installed for root bootstrap and the unprivileged deploy user."
  type        = string

  validation {
    condition     = can(regex("^ssh-ed25519 ", trimspace(var.ssh_public_key)))
    error_message = "ssh_public_key must be an Ed25519 OpenSSH public key."
  }
}

variable "ssh_allowed_cidrs" {
  description = "IPv4 or IPv6 CIDRs allowed to reach SSH. Public-anywhere access is rejected."
  type        = list(string)

  validation {
    condition = (
      length(var.ssh_allowed_cidrs) > 0 &&
      alltrue([for cidr in var.ssh_allowed_cidrs : can(cidrhost(cidr, 0))]) &&
      !contains(var.ssh_allowed_cidrs, "0.0.0.0/0") &&
      !contains(var.ssh_allowed_cidrs, "::/0")
    )
    error_message = "Provide at least one valid administrative CIDR; 0.0.0.0/0 and ::/0 are forbidden for SSH."
  }
}

variable "enable_backups" {
  description = "Enable Hetzner server backups; this has an additional cost."
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Protect the persistent demo server from accidental deletion. Disable deliberately before destroy."
  type        = bool
  default     = true
}

variable "enable_backup_object_storage" {
  description = "Create the private external backup bucket after its recurring cost is approved."
  type        = bool
  default     = false
}

variable "backup_object_storage_location" {
  description = "German Hetzner Object Storage location, intentionally separate from the demo server."
  type        = string
  default     = "fsn1"

  validation {
    condition     = contains(["fsn1", "nbg1"], var.backup_object_storage_location)
    error_message = "Backups must remain in a German Hetzner Object Storage location (fsn1 or nbg1)."
  }
}

variable "backup_object_storage_access_key" {
  description = "S3 access key used by Terraform to manage the private backup bucket."
  type        = string
  sensitive   = true
  default     = null
  nullable    = true

  validation {
    condition     = !var.enable_backup_object_storage || try(length(trimspace(var.backup_object_storage_access_key)) > 0, false)
    error_message = "An Object Storage access key is required when the backup bucket is enabled."
  }
}

variable "backup_object_storage_secret_key" {
  description = "S3 secret key used by Terraform to manage the private backup bucket."
  type        = string
  sensitive   = true
  default     = null
  nullable    = true

  validation {
    condition     = !var.enable_backup_object_storage || try(length(trimspace(var.backup_object_storage_secret_key)) > 0, false)
    error_message = "An Object Storage secret key is required when the backup bucket is enabled."
  }
}
