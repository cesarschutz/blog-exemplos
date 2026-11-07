package br.com.cesarschutz.exemplos.pin;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;

import static br.com.cesarschutz.exemplos.pin.PedidoRecusado.Motivo.*;

/**
 * O lado do servidor: confere o pedido e decifra o PIN.
 *
 * <p>A ordem das conferências importa. Primeiro o que não custa cifra (a chave existe, é deste integrador, o
 * instante está na janela); depois a decifração, em que a etiqueta do GCM recusa qualquer alteração na cifra, no
 * IV ou na AAD. O algoritmo vem da chave: o campo {@code algoritmo} do pedido só serve para recusar o pedido que
 * pede outra coisa, nunca para escolher como decifrar.
 *
 * <p>O que este código não resolve, e o capítulo diz: o PIN decifrado existe em claro na memória deste processo.
 * Isso é o que um HSM evita e o que tira este desenho do caminho do PCI PIN.
 */
public final class DecifradorDePin {

    private final Chaveiro chaveiro;
    private final Clock relogio;
    private final Duration janela;

    public DecifradorDePin(Chaveiro chaveiro, Clock relogio, Duration janela) {
        this.chaveiro = chaveiro;
        this.relogio = relogio;
        this.janela = janela;
    }

    // tag::decifra[]
    public char[] decifrar(String integrador, PedidoDeValidacao pedido) {
        var chave = chaveiro.porKid(pedido.kid())
                .orElseThrow(() -> new PedidoRecusado(CHAVE_DESCONHECIDA));
        if (!chave.integrador().equals(integrador)) throw new PedidoRecusado(CHAVE_DE_OUTRO_INTEGRADOR);
        // o algoritmo é o da chave; o do pedido só pode concordar com ele
        if (pedido.algoritmo() != null && !pedido.algoritmo().equals(chave.algoritmo().name()))
            throw new PedidoRecusado(ALGORITMO_RECUSADO);
        var idade = Duration.between(pedido.instante(), relogio.instant()).abs();
        if (idade.compareTo(janela) > 0) throw new PedidoRecusado(FORA_DA_JANELA);
        if (pedido.iv() == null || pedido.iv().length != CifradorDePin.TAMANHO_DO_IV)
            throw new PedidoRecusado(FORMATO_INVALIDO);
        byte[] claro = abrir(chave, pedido);
        // end::decifra[]
        return comoPin(claro);
    }

    private static byte[] abrir(Chave chave, PedidoDeValidacao pedido) {
        try {
            var aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.DECRYPT_MODE, chave.segredo(),
                    new GCMParameterSpec(CifradorDePin.BITS_DA_ETIQUETA, pedido.iv()));
            aes.updateAAD(Aad.de(chave.kid(), pedido.cartao(), pedido.instante()));
            return aes.doFinal(pedido.cifrado());
        } catch (AEADBadTagException e) {
            throw new PedidoRecusado(CIFRA_INVALIDA);
        } catch (GeneralSecurityException e) {
            throw new PedidoRecusado(CIFRA_INVALIDA);
        }
    }

    /**
     * O PIN tem de 4 a 12 dígitos. Os bytes viram um arranjo de caracteres sem passar por {@code String} (uma
     * {@code String} não pode ser apagada da memória) e são zerados em seguida. Isso encurta o tempo do PIN em claro;
     * não o tira da memória do processo, que é o limite do desenho.
     */
    static char[] comoPin(byte[] claro) {
        try {
            if (claro.length < 4 || claro.length > 12) throw new PedidoRecusado(PIN_INVALIDO);
            char[] pin = new char[claro.length];
            for (int i = 0; i < claro.length; i++) {
                if (claro[i] < '0' || claro[i] > '9') {
                    java.util.Arrays.fill(pin, '\0');
                    throw new PedidoRecusado(PIN_INVALIDO);
                }
                pin[i] = (char) claro[i];
            }
            return pin;
        } finally {
            java.util.Arrays.fill(claro, (byte) 0);
        }
    }
}
