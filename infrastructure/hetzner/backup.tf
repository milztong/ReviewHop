resource "minio_s3_bucket" "backup" {
  count = var.enable_backup_object_storage ? 1 : 0

  bucket_prefix  = "ankileet-demo-backup-"
  acl            = "private"
  object_locking = false

  lifecycle {
    prevent_destroy = true

    precondition {
      condition     = var.backup_object_storage_location != var.location
      error_message = "The logical backup bucket must use a different location than the demo server."
    }
  }
}
