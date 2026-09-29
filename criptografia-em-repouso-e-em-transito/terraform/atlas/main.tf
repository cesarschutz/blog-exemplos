# O Terraform do artigo ("MongoDB Atlas", em repouso). O teste roda "terraform validate": aplicar
# exige uma conta no Atlas. Onde o artigo mostra "# ... demais configurações do cluster", aqui estão
# os campos mínimos de um cluster M10 numa região só.

resource "mongodbatlas_encryption_at_rest" "this" {
  project_id = var.atlas_project_id

  aws_kms_config {
    enabled                = true
    customer_master_key_id = aws_kms_key.atlas.id
    region                 = "SA_EAST_1"
    role_id                = mongodbatlas_cloud_provider_access_authorization.atlas.role_id
  }
}

resource "mongodbatlas_advanced_cluster" "pedidos" {
  # o project_id vem do recurso acima, para o cluster esperar a criptografia estar configurada
  project_id                  = mongodbatlas_encryption_at_rest.this.project_id
  encryption_at_rest_provider = "AWS"

  name         = "pedidos"
  cluster_type = "REPLICASET"
  replication_specs = [{
    region_configs = [{
      provider_name = "AWS"
      region_name   = "SA_EAST_1"
      priority      = 7
      electable_specs = {
        instance_size = "M10"
        node_count    = 3
      }
    }]
  }]
}
