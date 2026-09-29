# O Terraform do artigo ("PostgreSQL no Amazon RDS", em repouso).
# O teste roda "terraform validate" e aplica este arquivo contra o moto (AWS simulada).

resource "aws_kms_key" "rds" {
  description = "Chave do RDS de pedidos"
}

resource "aws_db_instance" "pedidos" {
  identifier                  = "pedidos-db"
  engine                      = "postgres"
  instance_class              = "db.t4g.medium"
  allocated_storage           = 50
  username                    = "pgadmin"
  manage_master_user_password = true

  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn
}
