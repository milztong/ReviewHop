resource "hcloud_ssh_key" "deploy" {
  name       = "${local.name}-deploy"
  public_key = trimspace(var.ssh_public_key)
  labels     = local.labels
}

resource "hcloud_primary_ip" "demo" {
  name        = "${local.name}-ipv4"
  location    = var.location
  type        = "ipv4"
  auto_delete = false
  labels      = local.labels
}

resource "hcloud_firewall" "demo" {
  name   = "${local.name}-firewall"
  labels = local.labels

  rule {
    direction   = "in"
    protocol    = "tcp"
    port        = "22"
    source_ips  = var.ssh_allowed_cidrs
    description = "Administrative SSH from explicitly approved networks"
  }

  rule {
    direction   = "in"
    protocol    = "tcp"
    port        = "80"
    source_ips  = ["0.0.0.0/0", "::/0"]
    description = "HTTP for ACME challenge and HTTPS redirect"
  }

  rule {
    direction   = "in"
    protocol    = "tcp"
    port        = "443"
    source_ips  = ["0.0.0.0/0", "::/0"]
    description = "Public HTTPS application entry point"
  }

  rule {
    direction   = "in"
    protocol    = "icmp"
    source_ips  = ["0.0.0.0/0", "::/0"]
    description = "Path MTU discovery and network diagnostics"
  }
}

resource "hcloud_server" "demo" {
  name         = local.name
  location     = var.location
  server_type  = var.server_type
  image        = var.server_image
  ssh_keys     = [hcloud_ssh_key.deploy.id]
  firewall_ids = [hcloud_firewall.demo.id]

  backups                  = var.enable_backups
  delete_protection        = var.deletion_protection
  rebuild_protection       = var.deletion_protection
  shutdown_before_deletion = true
  labels                   = local.labels

  user_data = templatefile("${path.module}/cloud-init.yaml.tftpl", {
    ssh_public_key = trimspace(var.ssh_public_key)
  })

  public_net {
    ipv4_enabled = true
    ipv4         = hcloud_primary_ip.demo.id
    ipv6_enabled = false
  }
}
