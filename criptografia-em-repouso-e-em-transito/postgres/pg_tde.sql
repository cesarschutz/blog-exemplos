-- postgresql.conf: shared_preload_libraries = 'pg_tde' (e reinicie o servidor)
-- (no teste, o container sobe com "postgres -c shared_preload_libraries=pg_tde")
CREATE EXTENSION pg_tde;

-- configure o provedor de chaves (Vault, OpenBao ou KMIP) e a chave principal;
-- as funções mudam entre versões, então siga a documentação da versão instalada.
-- Aqui, com o pg_tde 2.2, um provedor de ARQUIVO LOCAL, que a Percona recomenda só para teste:
SELECT pg_tde_add_database_key_provider_file('arquivo-local', '/tmp/pg_tde_chaves');
SELECT pg_tde_create_key_using_database_key_provider('chave-principal', 'arquivo-local');
SELECT pg_tde_set_key_using_database_key_provider('chave-principal', 'arquivo-local');

CREATE TABLE cartao (
  id     bigint PRIMARY KEY,
  numero text NOT NULL
) USING tde_heap;
