# Criptografia em repouso e em trânsito — o que é e como ativar no Postgres e no MongoDB

Código do artigo **[Criptografia em repouso e em trânsito — o que é e como ativar no Postgres e no MongoDB](https://blog.cesarschutz.com.br/posts/criptografia-em-repouso-e-em-transito/)**.

As configurações do artigo rodando de verdade, em containers: o Postgres com TLS e o `pg_hba.conf` que recusa conexão sem TLS, o `sslmode` do cliente contra um servidor impostor, o `pg_tde` cifrando a tabela no disco, o MongoDB com `requireTLS`, o MongoDB Enterprise com criptografia em repouso, o Queryable Encryption e os comandos da AWS CLI e do Terraform contra uma AWS simulada.

## O que tem aqui

| Seção do artigo | Arquivos | Teste |
| --- | --- | --- |
| PostgreSQL no Amazon RDS, em repouso (CLI) | `aws/criar-instancia.sh`, `aws/migrar-instancia.sh` | `AwsCliTest` |
| PostgreSQL no Amazon RDS, em repouso (Terraform) | `terraform/rds/main.tf` | `TerraformTest` |
| PostgreSQL no Amazon RDS, em trânsito (`sslmode`, `pg_stat_ssl`) | `postgres/quem-usa-tls.sql` | `PostgresTlsTest` |
| PostgreSQL fora do RDS, em trânsito | `postgres/postgresql.conf`, `postgres/pg_hba.conf` | `PostgresTlsTest` |
| PostgreSQL fora do RDS, em repouso (`pg_tde`) | `postgres/pg_tde.sql` | `PgTdeTest` |
| MongoDB Atlas, em repouso (Terraform) | `terraform/atlas/main.tf` | `TerraformTest` (só `validate`) |
| MongoDB fora do Atlas, em trânsito | `mongo/mongod-tls.conf` | `MongoTlsTest` |
| MongoDB fora do Atlas, em repouso | `mongo/mongod-kmip.conf`, `mongo/mongod-arquivo-de-chave.conf` | `MongoEmRepousoTest` |
| E a criptografia em uso? (Queryable Encryption) | `mongo/queryable-encryption-aplicacao.js`, `mongo/queryable-encryption-dba.js` | `QueryableEncryptionTest` |
| E a criptografia em uso? (`pgcrypto` e o log) | — | `PostgresTlsTest` |

O que cada teste mostra:

- **Postgres com TLS** (`PostgresTlsTest`): sem TLS, o `pg_hba.conf` recusa; o padrão do driver JDBC é `sslmode=prefer`; `verify-full` conecta no banco de verdade e recusa um servidor impostor (com certificado de outra CA) e um nome que não está no certificado; `require` aceita o impostor, porque cifra mas não confere quem está do outro lado; a consulta em `pg_stat_ssl` mostra as conexões com TLS; e, com o log de comandos ligado, a chave do `pgcrypto` aparece no log do servidor.
- **`pg_tde`** (`PgTdeTest`): o número do cartão aparece no arquivo da tabela comum e não aparece no da tabela `USING tde_heap`; mesmo assim, quem tem usuário e senha lê o número normalmente.
- **MongoDB com TLS** (`MongoTlsTest`): sem TLS, o servidor recusa; com `tls=true` e `tlsCAFile`, como na string de conexão do artigo, conecta; sem a CA, o cliente não confia no certificado; sem a linha `allowConnectionsWithoutCertificates`, o cliente sem certificado é recusado.
- **MongoDB em repouso** (`MongoEmRepousoTest`): no Enterprise, o número não aparece em nenhum arquivo do `dbPath`; no Community, aparece em claro nos arquivos do WiredTiger e no journal, e a opção `--enableEncryption` nem existe.
- **Queryable Encryption** (`QueryableEncryptionTest`): a aplicação, com a chave, consulta pelo CPF e recebe o CPF aberto; o DBA, sem a chave, vê `cpf: Binary.createFromBase64('D…', 6)` e o campo `__safeContent__`, e a busca pelo CPF em claro não encontra nada.
- **AWS CLI** (`AwsCliTest`): a instância criada com `--storage-encrypted` e a chave pelo alias nasce criptografada; a instância que já existe sem criptografia passa pelo snapshot, pela cópia com a chave KMS e pela restauração, e a nova sai criptografada, com a chave do alias.
- **Terraform** (`TerraformTest`): o `aws_db_instance` do artigo é aplicado com `storage_encrypted = true` e a chave pelo ARN; o `mongodbatlas_encryption_at_rest` e o cluster passam no `terraform validate`.

## Requisitos

- Java 21 ou mais novo.
- Docker no ar: os testes sobem os containers sozinhos (Testcontainers).
- Acesso à internet na primeira vez, para baixar as imagens (cerca de 7 GB em disco, a maior parte do MongoDB e do Percona) e os provedores do Terraform.

Não precisa instalar o Gradle: o `gradlew` baixa a versão certa. Não precisa de conta na AWS nem no Atlas.

## Como rodar

```bash
./gradlew test
```

No Windows, `gradlew.bat test`. Para rodar uma parte só, por exemplo o Postgres com TLS:

```bash
./gradlew test --tests '*PostgresTlsTest'
```

## Resultado esperado

Os 28 testes passam, e cada um aparece como `PASSED` no terminal, com o nome dizendo o que ele confere. Com `-i`, o `PostgresTlsTest` imprime a saída da consulta em `pg_stat_ssl`, o `QueryableEncryptionTest` imprime o que a aplicação e o DBA veem, e o `AwsCliTest` imprime a tabela do `describe-db-instances`.

## Versões

- PostgreSQL 17.11 (imagem oficial `postgres:17`) e driver JDBC 42.7.13
- Percona Distribution for PostgreSQL 18.6 (`percona/percona-distribution-postgresql:18`), com `pg_tde` 2.2
- MongoDB 8.0.32 Community (`mongo:8.0`, com `mongosh` 2.11) e Enterprise (`mongodb/mongodb-enterprise-server:8.0-ubuntu2204`, com `mongosh` 2.6)
- AWS CLI 2.37.5 (`amazon/aws-cli:2.37.5`) e moto 5.2.2 (`motoserver/moto:5.2.2`)
- Terraform 1.16.4, provedores `hashicorp/aws` 6.66.0 e `mongodb/mongodbatlas` 2.18.0
- Testcontainers 2.0.5, JUnit 6.1.3, Bouncy Castle 1.86 (só para gerar os certificados de teste)
- Gradle 9.5.1
- Java 21

## Diferenças em relação ao artigo

As configurações e os comandos são os do artigo. O repositório só acrescenta o que o artigo não mostra, porque é do ambiente de teste:

- **Certificados:** as CAs e os certificados são gerados a cada execução (`Certificados.java`), sem chave privada no repositório. Uma segunda CA, a "impostora", faz o papel de alguém no meio do caminho.
- **Postgres:** o teste inclui o bloco do artigo (`postgres/postgresql.conf`) no fim do `postgresql.conf` do container, troca o `pg_hba.conf` pelo do artigo e instala o certificado e a chave com dono `postgres` e permissão `0600`.
- **`pg_tde`:** o `shared_preload_libraries` entra pela linha de comando do container (`postgres -c shared_preload_libraries=pg_tde`), e as chaves ficam num provedor de arquivo local, que a Percona recomenda só para teste. Em produção, o provedor é um Vault, OpenBao ou servidor KMIP, como no artigo.
- **MongoDB com TLS:** o `bindIp` é `0.0.0.0`, porque o `mongod` roda num container (no artigo, `127.0.0.1,10.0.1.15`), e o cliente é o `mongosh` do próprio container. O replica set e a migração de `allowTLS` para `requireTLS` não entram nos testes.
- **MongoDB em repouso:** o teste usa o `encryptionKeyFile` (`mongo/mongod-arquivo-de-chave.conf`), que não precisa de servidor KMIP; o bloco KMIP do artigo, o recomendado, está em `mongo/mongod-kmip.conf`. No teste, o `mongod` do Enterprise roda como root, para ler o arquivo de chave que o Testcontainers copia com dono root.
- **Queryable Encryption:** a chave mestra é "local" (96 bytes gerados no script), só para o teste; em produção, ela fica num KMS. O cliente é o `mongosh`, com o `mongocryptd` do próprio Enterprise, e o banco é um replica set de um membro só.
- **AWS:** a AWS é o moto, um simulador open source que confere a API (os parâmetros, o alias da chave, o que cada recurso devolve) mas **não cifra nada**: a criptografia do storage só acontece na AWS de verdade. A CLI aponta para ele pela variável `AWS_ENDPOINT_URL`, então os comandos ficam como no artigo. A chave KMS e o alias são criados antes (`aws/preparar-chave.sh`), e, para a migração, o teste cria antes a `pedidos-db` sem criptografia. No moto, o `describe-db-instances` mostra a chave pelo alias; na AWS, ele mostra o ARN.
- **Terraform:** no RDS, o teste acrescenta o provedor apontando para o moto (`src/test/resources/terraform/provedor-moto.tf`) e o `versions.tf` com a versão do provedor. No Atlas, onde o artigo mostra `# ... demais configurações do cluster`, estão os campos mínimos de um cluster M10, e `terraform/atlas/apoio.tf` traz o que o artigo não mostra: os provedores, a variável do projeto, a chave no AWS KMS e o acesso do Atlas à conta AWS.
- **Fora dos testes:** o LUKS (precisa de um disco e de privilégios no kernel), o pgBackRest, o `rds.force_ssl` (só existe no RDS) e o Atlas de verdade (precisa de conta).
- **Sem internet para o Terraform:** com a variável `TF_ESPELHO` apontando para um diretório no formato de *filesystem mirror* do Terraform, os provedores vêm dele. Sem a variável, eles vêm do registry, como no GitHub Actions.

Se o artigo mudar, este código muda junto.
