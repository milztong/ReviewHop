resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_assume_role" {
  statement {
    sid     = "GitHubStagingEnvironment"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [var.github_oidc_subject]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "${local.name_prefix}-github-deploy"
  description          = "Short-lived GitHub Actions deployment role for AnkiLeet staging"
  assume_role_policy   = data.aws_iam_policy_document.github_assume_role.json
  max_session_duration = 3600
  permissions_boundary = var.permissions_boundary_arn
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid = "TerraformStateBucket"
    actions = [
      "s3:ListBucket"
    ]
    resources = [aws_s3_bucket.terraform_state.arn]
  }

  statement {
    sid = "TerraformStateObjects"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject"
    ]
    resources = ["${aws_s3_bucket.terraform_state.arn}/ankileet/*"]
  }

  statement {
    sid = "PushApplicationImages"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories"
    ]
    resources = values(aws_ecr_repository.application)[*].arn
  }

  statement {
    sid       = "AuthenticateToEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "ManageStagingInfrastructure"
    actions = [
      "acm:*",
      "application-autoscaling:*",
      "budgets:*",
      "cloudwatch:*",
      "ec2:*",
      "ecs:*",
      "elasticloadbalancing:*",
      "logs:*",
      "rds:*",
      "route53:*",
      "servicediscovery:*",
      "secretsmanager:*",
      "sns:*"
    ]
    resources = ["*"]
  }

  statement {
    sid = "ManageAnkiLeetRoles"
    actions = [
      "iam:AttachRolePolicy",
      "iam:CreatePolicy",
      "iam:CreatePolicyVersion",
      "iam:CreateRole",
      "iam:DeletePolicy",
      "iam:DeletePolicyVersion",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:DetachRolePolicy",
      "iam:GetPolicy",
      "iam:GetPolicyVersion",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:ListPolicyVersions",
      "iam:ListRolePolicies",
      "iam:PutRolePolicy",
      "iam:TagPolicy",
      "iam:TagRole",
      "iam:UntagPolicy",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy"
    ]
    resources = [
      "arn:aws:iam::*:role/${local.name_prefix}-*",
      "arn:aws:iam::*:policy/${local.name_prefix}-*"
    ]
  }

  statement {
    sid       = "PassAnkiLeetTaskRoles"
    actions   = ["iam:PassRole"]
    resources = ["arn:aws:iam::*:role/${local.name_prefix}-*"]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid       = "CreateRequiredServiceLinkedRoles"
    actions   = ["iam:CreateServiceLinkedRole"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "iam:AWSServiceName"
      values = [
        "ecs.amazonaws.com",
        "elasticloadbalancing.amazonaws.com",
        "rds.amazonaws.com"
      ]
    }
  }

  statement {
    sid = "UseAnkiLeetPlatformKey"
    actions = [
      "kms:CreateGrant",
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:Encrypt",
      "kms:GenerateDataKey",
      "kms:ListGrants",
      "kms:RevokeGrant"
    ]
    resources = [aws_kms_key.platform.arn]
  }

  statement {
    sid       = "DiscoverAnkiLeetPlatformKey"
    actions   = ["kms:ListAliases"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "${local.name_prefix}-deployment"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}
