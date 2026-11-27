output "endpoint" {
  value = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "identifier" {
  value = aws_db_instance.this.identifier
}

output "master_secret_arn" {
  value     = aws_db_instance.this.master_user_secret[0].secret_arn
  sensitive = true
}

output "publicly_accessible" {
  value = aws_db_instance.this.publicly_accessible
}

output "storage_encrypted" {
  value = aws_db_instance.this.storage_encrypted
}

output "master_password_managed" {
  value = aws_db_instance.this.manage_master_user_password
}
