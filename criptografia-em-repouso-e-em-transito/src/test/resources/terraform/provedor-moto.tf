# Só no teste: o provedor da AWS apontando para o moto, com credenciais falsas.
provider "aws" {
  region                      = "sa-east-1"
  access_key                  = "teste"
  secret_key                  = "teste"
  skip_credentials_validation = true
  skip_requesting_account_id  = true
  skip_metadata_api_check     = true

  endpoints {
    kms            = "http://moto:5000"
    rds            = "http://moto:5000"
    secretsmanager = "http://moto:5000"
    sts            = "http://moto:5000"
  }
}
