locals {
  name = "ankileet-demo"

  labels = {
    project     = "ankileet"
    environment = "demo"
    managed_by  = "terraform"
    owner       = lower(replace(trimspace(var.owner), " ", "-"))
  }
}
