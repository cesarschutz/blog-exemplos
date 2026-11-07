package br.com.cesarschutz.exemplos.pin;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Os dados adicionais autenticados (AAD) do pedido: o kid, o cartão e o instante, numa ordem fixa.
 *
 * <p>Os dois lados precisam montar exatamente os mesmos bytes; por isso a montagem mora num lugar só. O separador
 * é uma barra vertical, que não aparece em nenhum dos três campos.
 */
final class Aad {

    private Aad() {
    }

    static byte[] de(String kid, String cartao, Instant instante) {
        return (kid + "|" + cartao + "|" + instante.getEpochSecond()).getBytes(StandardCharsets.US_ASCII);
    }
}
