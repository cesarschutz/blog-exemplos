package br.com.cesarschutz.exemplos.pin;

import javax.crypto.SecretKey;
import java.util.Objects;

/**
 * Uma chave de cifrar PIN, com o que o servidor precisa saber sobre ela.
 *
 * <p>O algoritmo mora aqui, junto da chave, e não no pedido: é o servidor que decide como decifrar, olhando a
 * chave, nunca o parâmetro que o cliente mandou (a confusão de algoritmo dos JWT nasce de deixar o cliente
 * escolher). Cada integrador tem as suas chaves; o {@code kid} identifica qual delas cifrou o pedido e permite
 * ter duas válidas ao mesmo tempo durante uma rotação.
 *
 * @param kid        o identificador da chave, que viaja em claro no pedido
 * @param integrador quem pode usar a chave (o servidor confere que o pedido veio dele)
 * @param algoritmo  o único algoritmo aceito com esta chave
 * @param segredo    a chave AES de 256 bits
 */
public record Chave(String kid, String integrador, Algoritmo algoritmo, SecretKey segredo) {

    /** Os algoritmos que o servidor conhece. Hoje, um só. */
    public enum Algoritmo {
        AES_256_GCM
    }

    public Chave {
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(integrador, "integrador");
        Objects.requireNonNull(algoritmo, "algoritmo");
        Objects.requireNonNull(segredo, "segredo");
        if (!"AES".equals(segredo.getAlgorithm()) || segredo.getEncoded().length != 32) {
            throw new IllegalArgumentException("A chave precisa ser AES de 256 bits");
        }
    }
}
