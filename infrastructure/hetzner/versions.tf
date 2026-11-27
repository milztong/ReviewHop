terraform {
  required_version = ">= 1.16.0, < 2.0.0"

  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.70.2"
    }
    minio = {
      source  = "aminueza/minio"
      version = "3.33.1"
    }
  }
}
