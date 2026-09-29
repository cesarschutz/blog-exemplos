# blog-exemplos

O código dos artigos do blog [blog.cesarschutz.com.br](https://blog.cesarschutz.com.br/).

Cada pasta é um exemplo completo e independente, com o mesmo nome (slug) do artigo na URL do blog. O código é o mesmo mostrado no artigo, com testes que conferem as saídas publicadas.

## Exemplos

| Artigo | Livro | Stack | Código |
| --- | --- | --- | --- |
| [Filtros de serialização no Jackson — mascarando número de cartão nos logs](https://blog.cesarschutz.com.br/posts/jackson-filtros-mascarando-cartao/) | Desenvolvimento de Software | Java 21, Spring Boot 4.1, Jackson 3.2 | [jackson-filtros-mascarando-cartao](jackson-filtros-mascarando-cartao/) |
| [Criptografia em repouso e em trânsito — o que é e como ativar no Postgres e no MongoDB](https://blog.cesarschutz.com.br/posts/criptografia-em-repouso-e-em-transito/) | Segurança | Java 21, Testcontainers, PostgreSQL 17, Percona 18 (`pg_tde`), MongoDB 8.0, AWS CLI e Terraform no moto | [criptografia-em-repouso-e-em-transito](criptografia-em-repouso-e-em-transito/) |

## Como rodar um exemplo

Entre na pasta do exemplo e siga o README dela. Em geral:

```bash
cd jackson-filtros-mascarando-cartao
./gradlew test
```

Quando o exemplo precisa de banco de dados ou de serviços da AWS, ele usa o Docker: ou a pasta traz um `docker-compose.yml`, ou os próprios testes sobem os containers (Testcontainers). O README da pasta explica.

## Como os exemplos são organizados

- **Uma pasta por artigo**, com o mesmo slug da URL do post.
- **Cada pasta é independente**: tem o próprio build, o próprio wrapper e as versões fixadas. Atualizar um exemplo não afeta os outros.
- **Dependências externas em containers**: Postgres, MongoDB, a AWS simulada (moto ou LocalStack) e o que mais for preciso, sem exigir contas em serviços, pelo Docker Compose ou pelo Testcontainers.
- **README com o mesmo formato** em todas as pastas: o que o exemplo demonstra, o link do artigo, requisitos, como rodar, resultado esperado, versões e as diferenças em relação ao artigo.
- **Testes no GitHub Actions**: a cada mudança, rodam os testes das pastas alteradas; uma vez por mês, rodam todos.

## Licença

O código deste repositório está sob a [licença MIT](LICENSE). O texto dos artigos, no blog, está sob [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/deed.pt-br).
