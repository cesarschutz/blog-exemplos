#!/usr/bin/env bash
# Os comandos do artigo ("PostgreSQL no Amazon RDS", em repouso): a instância criada já criptografada,
# e a conferência das instâncias que você já tem.
set -euo pipefail

aws rds create-db-instance \
  --db-instance-identifier pedidos-db \
  --engine postgres \
  --db-instance-class db.t4g.medium \
  --allocated-storage 50 \
  --master-username pgadmin \
  --manage-master-user-password \
  --storage-encrypted \
  --kms-key-id alias/rds-pedidos

aws rds describe-db-instances \
  --query "DBInstances[].{instancia:DBInstanceIdentifier,criptografada:StorageEncrypted,chave:KmsKeyId}" \
  --output table
