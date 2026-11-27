output "application_url" {
  value = "https://${var.domain_name}"
}

output "load_balancer_arn_suffix" {
  value = aws_lb.application.arn_suffix
}

output "frontend_target_group_arn_suffix" {
  value = aws_lb_target_group.frontend.arn_suffix
}

output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "frontend_service_name" {
  value = aws_ecs_service.frontend.name
}

output "backend_service_name" {
  value = aws_ecs_service.backend.name
}

output "https_enabled" {
  value = aws_lb_listener.https.protocol == "HTTPS"
}

output "frontend_tasks_private" {
  value = aws_ecs_service.frontend.network_configuration[0].assign_public_ip == false
}

output "backend_tasks_private" {
  value = aws_ecs_service.backend.network_configuration[0].assign_public_ip == false
}
