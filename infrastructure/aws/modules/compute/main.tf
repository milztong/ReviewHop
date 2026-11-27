data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_iam_role" "execution" {
  name               = "${var.name_prefix}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "database_secret" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.database_secret_arn]
  }

  statement {
    actions   = ["kms:Decrypt"]
    resources = [var.kms_key_arn]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["secretsmanager.${var.aws_region}.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "database_secret" {
  name   = "${var.name_prefix}-database-secret"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.database_secret.json
}

resource "aws_iam_role" "application" {
  name               = "${var.name_prefix}-application"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_cloudwatch_log_group" "application" {
  for_each = toset(["backend", "frontend"])

  name              = "/ankileet/staging/${each.key}"
  retention_in_days = var.log_retention_days
}

resource "aws_service_discovery_private_dns_namespace" "this" {
  name = "ankileet.internal"
  vpc  = var.vpc_id
}

resource "aws_service_discovery_service" "backend" {
  name = "api"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.this.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {}
}

resource "aws_acm_certificate" "application" {
  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "certificate_validation" {
  for_each = {
    for option in aws_acm_certificate.application.domain_validation_options : option.domain_name => {
      name   = option.resource_record_name
      record = option.resource_record_value
      type   = option.resource_record_type
    }
  }

  zone_id = var.hosted_zone_id
  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  ttl     = 60
}

resource "aws_acm_certificate_validation" "application" {
  certificate_arn         = aws_acm_certificate.application.arn
  validation_record_fqdns = [for record in aws_route53_record.certificate_validation : record.fqdn]
}

# Public exposure is intentional: ADR-0010 defines the ALB as the only internet edge.
#trivy:ignore:AVD-AWS-0053
resource "aws_lb" "application" {
  name                       = "${var.name_prefix}-alb"
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [var.load_balancer_security_group_id]
  subnets                    = var.public_subnet_ids
  enable_deletion_protection = false
  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "frontend" {
  name        = "${var.name_prefix}-web"
  port        = 3000
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  deregistration_delay = 30

  health_check {
    enabled             = true
    path                = "/"
    protocol            = "HTTP"
    matcher             = "200-399"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.application.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.application.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.application.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

resource "aws_route53_record" "application" {
  zone_id = var.hosted_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.application.dns_name
    zone_id                = aws_lb.application.zone_id
    evaluate_target_health = true
  }
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.application.arn

  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode([{
    name      = "backend"
    image     = "${var.backend_repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{
      name          = "http"
      containerPort = 8080
      protocol      = "tcp"
      appProtocol   = "http"
    }]

    environment = [{
      name  = "ANKILEET_DB_URL"
      value = "jdbc:postgresql://${var.database_endpoint}:${var.database_port}/${var.database_name}?sslmode=require"
    }]

    secrets = [
      {
        name      = "ANKILEET_DB_USERNAME"
        valueFrom = "${var.database_secret_arn}:username::"
      },
      {
        name      = "ANKILEET_DB_PASSWORD"
        valueFrom = "${var.database_secret_arn}:password::"
      }
    ]

    healthCheck = {
      command     = ["CMD-SHELL", "wget --quiet --spider http://127.0.0.1:8080/actuator/health/readiness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 30
    }

    linuxParameters = {
      initProcessEnabled = true
    }

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.application["backend"].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${var.name_prefix}-backend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  enable_ecs_managed_tags            = true
  propagate_tags                     = "SERVICE"
  wait_for_steady_state              = true

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.application_subnet_ids
    security_groups  = [var.backend_security_group_id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.backend.arn
  }

  depends_on = [
    aws_iam_role_policy.database_secret,
    aws_iam_role_policy_attachment.execution
  ]
}

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${var.name_prefix}-frontend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.application.arn

  runtime_platform {
    cpu_architecture        = "X86_64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode([{
    name      = "frontend"
    image     = "${var.frontend_repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{
      name          = "http"
      containerPort = 3000
      protocol      = "tcp"
      appProtocol   = "http"
    }]

    environment = [{
      name  = "ANKILEET_API_BASE_URL"
      value = "http://api.ankileet.internal:8080"
    }]

    healthCheck = {
      command     = ["CMD-SHELL", "wget --quiet --spider http://127.0.0.1:3000 || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 15
    }

    linuxParameters = {
      initProcessEnabled = true
    }

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.application["frontend"].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "frontend" {
  name            = "${var.name_prefix}-frontend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = 60
  enable_ecs_managed_tags            = true
  propagate_tags                     = "SERVICE"
  wait_for_steady_state              = true

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.application_subnet_ids
    security_groups  = [var.frontend_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend.arn
    container_name   = "frontend"
    container_port   = 3000
  }

  depends_on = [
    aws_ecs_service.backend,
    aws_iam_role_policy_attachment.execution,
    aws_lb_listener.https
  ]
}
