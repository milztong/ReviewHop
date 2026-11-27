output "server_id" {
  description = "Hetzner Cloud server ID."
  value       = hcloud_server.demo.id
}

output "ipv4_address" {
  description = "Stable IPv4 address for the demo DNS A record."
  value       = hcloud_primary_ip.demo.ip_address
}

output "ssh_target" {
  description = "Deployment SSH target. Host-key verification remains mandatory."
  value       = "deploy@${hcloud_primary_ip.demo.ip_address}"
}

output "backup_bucket_name" {
  description = "Private logical-backup bucket name when Object Storage is enabled."
  value       = try(minio_s3_bucket.backup[0].bucket, null)
}
