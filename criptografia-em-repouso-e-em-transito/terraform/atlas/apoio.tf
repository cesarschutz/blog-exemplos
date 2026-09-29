# O que o artigo não mostra: os provedores, a variável do projeto, a chave no AWS KMS e o acesso do
# Atlas à conta AWS (o IAM role que ele assume). Na conta de verdade, o role precisa de kms:Encrypt,
# kms:Decrypt e kms:DescribeKey na chave.

terraform {
  required_providers {
    mongodbatlas = {
      source  = "mongodb/mongodbatlas"
      version = "2.18.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "6.66.0"
    }
  }
}

variable "atlas_project_id" {
  type = string
}

resource "aws_kms_key" "atlas" {
  description = "Chave do Atlas"
}

resource "mongodbatlas_cloud_provider_access_setup" "atlas" {
  project_id    = var.atlas_project_id
  provider_name = "AWS"
}

resource "mongodbatlas_cloud_provider_access_authorization" "atlas" {
  project_id = var.atlas_project_id
  role_id    = mongodbatlas_cloud_provider_access_setup.atlas.role_id

  aws {
    iam_assumed_role_arn = "arn:aws:iam::111122223333:role/atlas-kms"
  }
}
