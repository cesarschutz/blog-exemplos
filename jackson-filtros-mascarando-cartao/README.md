# Filtros de serialização no Jackson — mascarando número de cartão nos logs

Código do artigo **[Filtros de serialização no Jackson — mascarando número de cartão nos logs](https://blog.cesarschutz.com.br/posts/jackson-filtros-mascarando-cartao/)**.

Um mapper de log que mascara o número do cartão e remove o CVV, com `@JsonFilter`, `PropertyFilter` e um mixin em `Object`, enquanto o mapper da API continua devolvendo tudo.

## O que tem aqui

| Seção do artigo | Código | Teste |
| --- | --- | --- |
| 2. Removendo o CVV | `secao2/Cartao.java` | `secao2/RemovendoCvvTest` |
| 3. Mascarando o número | `MascaraCartaoFilter.java` | `secao2/MascarandoNumeroTest`, `MascararTest` |
| 4. Mixin em `Object` | `LogFilterMixin.java`, `secao4/` | `secao4/MixinEmObjectTest` |
| 5. Juntando tudo no Spring | `JacksonConfig.java`, `LogJson.java`, `builderdoboot/JacksonConfig.java` | `spring/` |
| 6. Anotação própria | `NumeroCartao.java`, `MascaraPorAnotacaoFilter.java`, `secao6/` | `secao6/AnotacaoPropriaTest` |
| 7. Cuidados | — | `secao7/CuidadosTest` |

Os testes conferem as saídas mostradas no artigo, caractere por caractere.

## Requisitos

- Java 21 ou mais novo.

Não precisa instalar o Gradle: o `gradlew` baixa a versão certa.

## Como rodar

```bash
./gradlew test
```

No Windows, `gradlew.bat test`.

## Resultado esperado

Todos os testes passam, e cada um aparece como `PASSED` no terminal. O `VersaoJacksonTest` confirma que o Jackson em uso é o 3.2.3.

## Versões

- Spring Boot 4.1.1
- Jackson 3.2.3 (`tools.jackson`), com `jackson-annotations` 2.22
- Gradle 9.5.1
- Java 21

## Diferenças em relação ao artigo

O código é o mesmo do artigo. O repositório só completa o que o artigo omite para caber no texto:

- a linha `package` em cada arquivo;
- os `import` dos trechos que o artigo mostra sem eles (`Pedido`, `MascaraPorAnotacaoFilter`, `secao6/Cartao` e a variação do `JacksonConfig`);
- em `Conta`, o construtor e os getters, no lugar do comentário `// getters...`;
- a variação do `JacksonConfig` da seção 5 (a que recebe o `JsonMapper.Builder`), que o artigo mostra só com os métodos, está completa em `builderdoboot/JacksonConfig.java`;
- as três versões de `Cartao` do artigo ficam em pacotes separados (`secao2`, `secao4` e `secao6`), porque o artigo usa uma diferente em cada seção;
- os trechos soltos, como a criação dos mappers e o `log.info(...)`, estão dentro dos testes.

Se o artigo mudar, este código muda junto.
