#!/usr/bin/env bash
# Os comandos do artigo para a instância que já existe sem criptografia: snapshot, cópia criptografada
# com a chave KMS e uma instância nova restaurada a partir da cópia.
set -euo pipefail

aws rds create-db-snapshot \
  --db-instance-identifier pedidos-db \
  --db-snapshot-identifier pedidos-sem-cripto

aws rds wait db-snapshot-available --db-snapshot-identifier pedidos-sem-cripto

aws rds copy-db-snapshot \
  --source-db-snapshot-identifier pedidos-sem-cripto \
  --target-db-snapshot-identifier pedidos-cripto \
  --kms-key-id alias/rds-pedidos

aws rds wait db-snapshot-available --db-snapshot-identifier pedidos-cripto

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier pedidos-db-v2 \
  --db-snapshot-identifier pedidos-cripto
