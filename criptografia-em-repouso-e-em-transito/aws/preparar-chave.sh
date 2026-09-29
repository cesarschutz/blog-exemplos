#!/usr/bin/env bash
# Preparação (não está no artigo): a chave KMS gerenciada por você, com o alias que o artigo usa.
set -euo pipefail
chave=$(aws kms create-key --description "Chave do RDS de pedidos" --query KeyMetadata.KeyId --output text)
aws kms create-alias --alias-name alias/rds-pedidos --target-key-id "$chave"
