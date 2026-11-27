module "network" {
  source = "../modules/network"

  name_prefix              = local.name_prefix
  vpc_cidr                 = var.vpc_cidr
  availability_zones       = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnet_cidrs      = var.public_subnet_cidrs
  application_subnet_cidrs = var.application_subnet_cidrs
  database_subnet_cidrs    = var.database_subnet_cidrs
}

resource "aws_security_group" "load_balancer" {
  name        = "${local.name_prefix}-load-balancer"
  description = "Public HTTPS entry point"
  vpc_id      = module.network.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "load_balancer_http" {
  security_group_id = aws_security_group.load_balancer.id
  description       = "HTTP redirect to HTTPS"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "load_balancer_https" {
  security_group_id = aws_security_group.load_balancer.id
  description       = "Public HTTPS"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_security_group" "frontend" {
  name        = "${local.name_prefix}-frontend"
  description = "Frontend ECS tasks"
  vpc_id      = module.network.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "load_balancer_frontend" {
  security_group_id            = aws_security_group.load_balancer.id
  description                  = "Forward traffic to frontend tasks"
  ip_protocol                  = "tcp"
  from_port                    = 3000
  to_port                      = 3000
  referenced_security_group_id = aws_security_group.frontend.id
}

resource "aws_vpc_security_group_ingress_rule" "frontend_load_balancer" {
  security_group_id            = aws_security_group.frontend.id
  description                  = "Only the ALB reaches Next.js"
  ip_protocol                  = "tcp"
  from_port                    = 3000
  to_port                      = 3000
  referenced_security_group_id = aws_security_group.load_balancer.id
}

resource "aws_vpc_security_group_egress_rule" "frontend_backend" {
  security_group_id            = aws_security_group.frontend.id
  description                  = "BFF calls the internal Java API"
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
  referenced_security_group_id = aws_security_group.backend.id
}

resource "aws_security_group" "aws_endpoints" {
  name        = "${local.name_prefix}-aws-endpoints"
  description = "Private AWS service endpoints"
  vpc_id      = module.network.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "aws_endpoints_frontend" {
  security_group_id            = aws_security_group.aws_endpoints.id
  description                  = "Frontend reaches private AWS APIs"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = aws_security_group.frontend.id
}

resource "aws_vpc_security_group_ingress_rule" "aws_endpoints_backend" {
  security_group_id            = aws_security_group.aws_endpoints.id
  description                  = "Backend reaches private AWS APIs"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = aws_security_group.backend.id
}

resource "aws_vpc_endpoint" "application_services" {
  for_each = toset([
    "ecr.api",
    "ecr.dkr",
    "logs",
    "secretsmanager"
  ])

  vpc_id              = module.network.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  private_dns_enabled = true
  subnet_ids          = module.network.application_subnet_ids
  security_group_ids  = [aws_security_group.aws_endpoints.id]

  tags = {
    Name = "${local.name_prefix}-${replace(each.key, ".", "-")}"
  }
}

resource "aws_vpc_security_group_egress_rule" "frontend_aws_endpoints" {
  security_group_id            = aws_security_group.frontend.id
  description                  = "TLS access to private AWS endpoints"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = aws_security_group.aws_endpoints.id
}

resource "aws_vpc_security_group_egress_rule" "frontend_s3" {
  security_group_id = aws_security_group.frontend.id
  description       = "Download ECR image layers from S3"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  prefix_list_id    = module.network.s3_prefix_list_id
}

resource "aws_vpc_security_group_egress_rule" "frontend_dns_udp" {
  security_group_id = aws_security_group.frontend.id
  description       = "VPC DNS resolver"
  ip_protocol       = "udp"
  from_port         = 53
  to_port           = 53
  cidr_ipv4         = local.vpc_dns_resolver_cidr
}

resource "aws_vpc_security_group_egress_rule" "frontend_dns_tcp" {
  security_group_id = aws_security_group.frontend.id
  description       = "VPC DNS resolver fallback"
  ip_protocol       = "tcp"
  from_port         = 53
  to_port           = 53
  cidr_ipv4         = local.vpc_dns_resolver_cidr
}

resource "aws_security_group" "backend" {
  name        = "${local.name_prefix}-backend"
  description = "Backend ECS tasks"
  vpc_id      = module.network.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "backend_frontend" {
  security_group_id            = aws_security_group.backend.id
  description                  = "Only the BFF reaches the Java API"
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
  referenced_security_group_id = aws_security_group.frontend.id
}

resource "aws_vpc_security_group_egress_rule" "backend_database" {
  security_group_id            = aws_security_group.backend.id
  description                  = "JDBC access to PostgreSQL"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.database.id
}

resource "aws_vpc_security_group_egress_rule" "backend_aws_endpoints" {
  security_group_id            = aws_security_group.backend.id
  description                  = "TLS access to private AWS endpoints"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = aws_security_group.aws_endpoints.id
}

resource "aws_vpc_security_group_egress_rule" "backend_s3" {
  security_group_id = aws_security_group.backend.id
  description       = "Download ECR image layers from S3"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  prefix_list_id    = module.network.s3_prefix_list_id
}

resource "aws_vpc_security_group_egress_rule" "backend_dns_udp" {
  security_group_id = aws_security_group.backend.id
  description       = "VPC DNS resolver"
  ip_protocol       = "udp"
  from_port         = 53
  to_port           = 53
  cidr_ipv4         = local.vpc_dns_resolver_cidr
}

resource "aws_vpc_security_group_egress_rule" "backend_dns_tcp" {
  security_group_id = aws_security_group.backend.id
  description       = "VPC DNS resolver fallback"
  ip_protocol       = "tcp"
  from_port         = 53
  to_port           = 53
  cidr_ipv4         = local.vpc_dns_resolver_cidr
}

resource "aws_security_group" "database" {
  name        = "${local.name_prefix}-database"
  description = "Private PostgreSQL access"
  vpc_id      = module.network.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "database_backend" {
  security_group_id            = aws_security_group.database.id
  description                  = "Only the Java API reaches PostgreSQL"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.backend.id
}
