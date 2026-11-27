resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.name_prefix}-igw"
  }
}

resource "aws_subnet" "public" {
  count = 2

  vpc_id                  = aws_vpc.this.id
  availability_zone       = var.availability_zones[count.index]
  cidr_block              = var.public_subnet_cidrs[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "${var.name_prefix}-public-${count.index + 1}"
    Tier = "public"
  }
}

resource "aws_subnet" "application" {
  count = 2

  vpc_id            = aws_vpc.this.id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = var.application_subnet_cidrs[count.index]

  tags = {
    Name = "${var.name_prefix}-application-${count.index + 1}"
    Tier = "application"
  }
}

resource "aws_subnet" "database" {
  count = 2

  vpc_id            = aws_vpc.this.id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = var.database_subnet_cidrs[count.index]

  tags = {
    Name = "${var.name_prefix}-database-${count.index + 1}"
    Tier = "database"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "${var.name_prefix}-public"
  }
}

resource "aws_route_table_association" "public" {
  count = 2

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "application" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.name_prefix}-application-private"
  }
}

resource "aws_route_table_association" "application" {
  count = 2

  subnet_id      = aws_subnet.application[count.index].id
  route_table_id = aws_route_table.application.id
}

resource "aws_route_table" "database" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.name_prefix}-database"
  }
}

resource "aws_route_table_association" "database" {
  count = 2

  subnet_id      = aws_subnet.database[count.index].id
  route_table_id = aws_route_table.database.id
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${data.aws_region.current.region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids = [
    aws_route_table.application.id,
    aws_route_table.database.id
  ]

  tags = {
    Name = "${var.name_prefix}-s3"
  }
}

data "aws_region" "current" {}
