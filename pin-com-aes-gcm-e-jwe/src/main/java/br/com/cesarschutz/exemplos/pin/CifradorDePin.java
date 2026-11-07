package br.com.cesarschutz.exemplos.pin;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;

/**
 * O lado do integrador: cifra o PIN com AES-256-GCM antes de mandar o pedido.
 *
 * <p>Três decisões, todas do NIST SP 800-38D: o IV tem 96 bits (12 bytes), o tamanho recomendado; ele é sorteado a
 * cada pedido, porque repetir um IV com a mesma chave quebra a autenticação do GCM; e a etiqueta tem 128 bits. O
 * cartão, o instante e o kid entram como AAD: não são cifrados (a API precisa deles), mas ficam amarrados à cifra.
 */
public final class CifradorDePin {

    static final int TAMANHO_DO_IV = 12;
    static final int BITS_DA_ETIQUETA = 128;

    private final SecureRandom sorteio;
    private final Clock relogio;

    public CifradorDePin(SecureRandom sorteio, Clock relogio) {
        this.sorteio = sorteio;
        this.relogio = relogio;
    }

    // tag::cifra[]
    public PedidoDeValidacao cifrar(Chave chave, String pin, String cartao) {
        byte[] iv = new byte[TAMANHO_DO_IV];
        sorteio.nextBytes(iv); // um IV novo por pedido, nunca reaproveitado
        var instante = relogio.instant();
        try {
            var aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, chave.segredo(), new GCMParameterSpec(BITS_DA_ETIQUETA, iv));
            aes.updateAAD(Aad.de(chave.kid(), cartao, instante)); // amarra cartão, instante e kid
            byte[] cifrado = aes.doFinal(pin.getBytes(StandardCharsets.US_ASCII));
            return new PedidoDeValidacao(chave.kid(), cartao, instante, iv, cifrado, null);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM indisponível nesta JVM", e);
        }
    }
    // end::cifra[]
}
