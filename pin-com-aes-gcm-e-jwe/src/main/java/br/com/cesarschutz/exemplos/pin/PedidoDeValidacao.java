package br.com.cesarschutz.exemplos.pin;

import java.time.Instant;

/**
 * O que o integrador manda à API de validação de senha, além do que o TLS já protege.
 *
 * <p>O número do cartão e o instante viajam em claro (a API precisa deles), mas entram na AAD da cifra: se alguém
 * trocar o cartão ou o instante no caminho, o servidor não consegue decifrar. O {@code algoritmo} é o campo que o
 * cliente pode mandar e que o servidor não usa para decidir nada (veja {@link DecifradorDePin}).
 *
 * @param kid       qual chave cifrou
 * @param cartao    o número do cartão (PAN)
 * @param instante  quando o integrador cifrou
 * @param iv        o vetor de inicialização, 12 bytes aleatórios, um por pedido
 * @param cifrado   o PIN cifrado seguido da etiqueta de autenticação (16 bytes)
 * @param algoritmo o algoritmo que o cliente diz ter usado (pode ser nulo)
 */
public record PedidoDeValidacao(String kid, String cartao, Instant instante, byte[] iv, byte[] cifrado, String algoritmo) {
}
