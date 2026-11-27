provider "hcloud" {}

provider "minio" {
  minio_server   = "${var.backup_object_storage_location}.your-objectstorage.com"
  minio_user     = coalesce(var.backup_object_storage_access_key, "not-configured")
  minio_password = coalesce(var.backup_object_storage_secret_key, "not-configured")
  minio_region   = var.backup_object_storage_location
  minio_ssl      = true
}
