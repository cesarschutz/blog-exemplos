# PIN na camada de aplicação — AES-GCM e JWE quando não há PIN block

Ligado a: **[PIN na camada de aplicação — AES-GCM e JWE quando não há PIN block](https://blog.cesarschutz.com.br/posts/pin-com-aes-gcm-e-jwe/)**, capítulo 7 do livro "Proteção do PIN, do terminal ao emissor".

O cenário: a outra ponta só oferece uma API REST que recebe a senha do cartão em claro, sob TLS. Este exemplo mostra a camada que dá para pôr por cima: o PIN cifrado com AES-256-GCM, com um IV aleatório por pedido, o cartão, o instante e o `kid` amarrados pela AAD, uma chave por integrador, rotação pelo `kid` e o servidor escolhendo o algoritmo pela chave, nunca pelo pedido. E o mesmo desenho no formato pronto, o JWE com `alg: "dir"` e `enc: "A256GCM"`.

O que ele **não** faz, e o capítulo 8 explica: o PIN decifrado existe em claro na memória do servidor. Nada aqui substitui um PIN block traduzido dentro de um HSM.

## O que tem aqui

| Seção do artigo | Código | Teste |
| --- | --- | --- |
| A chave, o integrador e o algoritmo | `Chave.java`, `Chaveiro.java` | `AesGcmTest` (rotação, outro integrador) |
| Cifrar no integrador | `CifradorDePin.java`, `Aad.java` | `AesGcmTest` (IV único, mesmo PIN sai diferente) |
| Conferir e decifrar no servidor | `DecifradorDePin.java`, `PedidoRecusado.java` | `AesGcmTest` (cartão trocado, instante fora da janela, algoritmo pedido pelo cliente) |
| O formato pronto: JWE `dir` + `A256GCM` | `JweDePin.java`, `JsonPlano.java` | `JweTest`, inclusive o exemplo oficial da RFC 7520, seção 5.6 |

O código principal usa só o JDK (`javax.crypto`), para cada decisão ficar à vista. Num sistema de verdade, use uma biblioteca de JOSE madura (Nimbus JOSE + JWT, jose4j) para o JWE; o teste `aConstrucaoBateComOExemploDaRfc7520` confere que a construção daqui é a da RFC, byte a byte.

## Requisitos

- Java 21 ou mais novo.

Não precisa instalar o Gradle: o `gradlew` baixa a versão certa.

## Como rodar

```bash
./gradlew test
```

No Windows, `gradlew.bat test`.

## Resultado esperado

Os 23 testes passam (13 em `AesGcmTest` e 10 em `JweTest`), cada um como `PASSED` no terminal.

## Versões

- Java 21 (o código não usa nada além do JDK)
- JUnit 6.1.3, só nos testes
- Gradle 9.5.1

## Diferenças em relação ao artigo

O código é o mesmo do artigo, que mostra só os trechos marcados com `tag::` e `end::`. O repositório completa o que o artigo omite: a linha `package`, os `import`, o leitor de JSON plano do JWE e os testes.

Se o artigo mudar, este código muda junto.
