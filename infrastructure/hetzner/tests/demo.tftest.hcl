mock_provider "hcloud" {
  override_resource {
    target = hcloud_primary_ip.demo
    values = {
      id         = "1001"
      ip_address = "203.0.113.42"
    }
  }
}

mock_provider "minio" {}

variables {
  owner               = "portfolio-owner"
  ssh_public_key      = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAITestOnlyKey ankileet-test"
  ssh_allowed_cidrs   = ["198.51.100.23/32"]
  enable_backups      = true
  deletion_protection = true
}

run "secure_single_server_baseline" {
  command = plan

  assert {
    condition     = hcloud_server.demo.server_type == "cx23"
    error_message = "The demo must start with the cost-conscious x86 server type."
  }

  assert {
    condition     = hcloud_server.demo.backups
    error_message = "The persistent demo must enable provider-level backups by default."
  }

  assert {
    condition     = hcloud_server.demo.delete_protection && hcloud_server.demo.rebuild_protection
    error_message = "The persistent demo must be protected from accidental deletion and rebuild."
  }

  assert {
    condition = alltrue([
      for rule in hcloud_firewall.demo.rule :
      rule.port != "22" || (!contains(rule.source_ips, "0.0.0.0/0") && !contains(rule.source_ips, "::/0"))
    ])
    error_message = "SSH must never be open to the entire internet."
  }

  assert {
    condition     = hcloud_primary_ip.demo.auto_delete == false
    error_message = "The public demo IP must survive an intentional server rebuild."
  }
}

run "private_backup_bucket_in_separate_location" {
  command = plan

  variables {
    enable_backup_object_storage     = true
    backup_object_storage_location   = "fsn1"
    backup_object_storage_access_key = "terraform-test-access-key"
    backup_object_storage_secret_key = "terraform-test-secret-key"
  }

  assert {
    condition     = minio_s3_bucket.backup[0].acl == "private"
    error_message = "The logical backup bucket must never be public."
  }

  assert {
    condition     = minio_s3_bucket.backup[0].object_locking == false
    error_message = "Restic retention must remain the single snapshot lifecycle authority."
  }
}

run "reject_backup_bucket_in_server_location" {
  command = plan

  variables {
    enable_backup_object_storage     = true
    backup_object_storage_location   = "nbg1"
    backup_object_storage_access_key = "terraform-test-access-key"
    backup_object_storage_secret_key = "terraform-test-secret-key"
  }

  expect_failures = [minio_s3_bucket.backup[0]]
}
