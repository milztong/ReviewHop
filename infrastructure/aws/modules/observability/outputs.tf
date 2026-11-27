output "alarm_topic_arn" {
  value = aws_sns_topic.alarms.arn
}

output "budget_name" {
  value = aws_budgets_budget.monthly.name
}

output "monthly_budget_usd" {
  value = tonumber(aws_budgets_budget.monthly.limit_amount)
}
