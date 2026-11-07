package br.com.cesarschutz.exemplos.pin;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static br.com.cesarschutz.exemplos.pin.PedidoRecusado.Motivo.*;

/**
 * O mesmo desenho, no formato pronto do JWE (RFC 7516), com {@code alg: "dir"} e {@code enc: "A256GCM"} (RFC 7518).
 *
 * <p>"dir" quer dizer que a chave combinada com o integrador cifra o conteúdo direto, sem chave embrulhada: a
 * segunda das cinco partes do JWE fica vazia. O cabeçalho protegido (com o {@code kid} e o instante {@code iat})
 * é a AAD do GCM, então ninguém o troca sem quebrar a etiqueta; o cartão vai dentro do conteúdo cifrado, junto do
 * PIN, e o servidor confere que é o mesmo cartão do pedido.
 *
 * <p>Na leitura, o servidor procura a chave pelo {@code kid} e exige que {@code alg} e {@code enc} sejam
 * exatamente os da chave (RFC 8725, seção 3.1). Um JWE que peça outra coisa é recusado antes de qualquer cifra.
 *
 * <p>Um custo do formato: o conteúdo é JSON, e ler JSON cria {@code String} com o PIN, que não se apaga da memória.
 * O caminho sem JWE ({@link DecifradorDePin}) evita isso; aqui, o PIN em claro dura mais na memória do processo.
 */
public final class JweDePin {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();

    private final SecureRandom sorteio;
    private final Clock relogio;

    public JweDePin(SecureRandom sorteio, Clock relogio) {
        this.sorteio = sorteio;
        this.relogio = relogio;
    }

    // tag::jwe-gera[]
    public String gerar(Chave chave, String pin, String cartao) {
        var cabecalho = new LinkedHashMap<String, Object>();
        cabecalho.put("alg", "dir");
        cabecalho.put("enc", "A256GCM");
        cabecalho.put("kid", chave.kid());
        cabecalho.put("iat", relogio.instant().getEpochSecond());
        String protegido = B64.encodeToString(JsonPlano.escrever(cabecalho).getBytes(StandardCharsets.UTF_8));

        var conteudo = new LinkedHashMap<String, Object>();
        conteudo.put("pin", pin);
        conteudo.put("cartao", cartao);
        byte[] iv = new byte[12];
        sorteio.nextBytes(iv); // um IV novo por JWE
        return compactar(chave.segredo(), protegido, iv, JsonPlano.escrever(conteudo).getBytes(StandardCharsets.UTF_8));
    }
    // end::jwe-gera[]

    /**
     * A serialização compacta de um JWE "dir" com AES-GCM (RFC 7516, seções 5.1 e 7.1): cabeçalho . chave embrulhada
     * (vazia no "dir") . IV . texto cifrado . etiqueta. Separada da política para o teste conferir a construção
     * contra o exemplo oficial da RFC 7520, seção 5.6.
     */
    static String compactar(SecretKey chave, String protegido, byte[] iv, byte[] claro) {
        byte[] cifradoComEtiqueta = gcm(Cipher.ENCRYPT_MODE, chave, iv, protegido, claro);
        int n = cifradoComEtiqueta.length - 16;
        return protegido + ".." + B64.encodeToString(iv) + "."
                + B64.encodeToString(java.util.Arrays.copyOf(cifradoComEtiqueta, n)) + "."
                + B64.encodeToString(java.util.Arrays.copyOfRange(cifradoComEtiqueta, n, n + 16));
    }

    /** O caminho inverso, sem política nenhuma: só a mecânica do formato. O servidor usa o {@link Leitor}. */
    static byte[] abrirCompacto(SecretKey chave, String jwe) {
        String[] p = jwe.split("\\.", -1);
        if (p.length != 5 || !p[1].isEmpty()) throw new PedidoRecusado(FORMATO_INVALIDO);
        byte[] iv = decodificar(p[2]);
        byte[] cifrado = decodificar(p[3]);
        byte[] etiqueta = decodificar(p[4]);
        if (iv.length != 12 || etiqueta.length != 16) throw new PedidoRecusado(FORMATO_INVALIDO);
        byte[] junto = java.util.Arrays.copyOf(cifrado, cifrado.length + 16);
        System.arraycopy(etiqueta, 0, junto, cifrado.length, 16);
        return gcm(Cipher.DECRYPT_MODE, chave, iv, p[0], junto);
    }

    /** O servidor: confere o cabeçalho contra a chave e só então decifra. */
    public static final class Leitor {
        private final Chaveiro chaveiro;
        private final Clock relogio;
        private final Duration janela;

        public Leitor(Chaveiro chaveiro, Clock relogio, Duration janela) {
            this.chaveiro = chaveiro;
            this.relogio = relogio;
            this.janela = janela;
        }

        // tag::jwe-le[]
        public char[] ler(String integrador, String cartaoDoPedido, String jwe) {
            String[] p = jwe.split("\\.", -1);
            if (p.length != 5 || !p[1].isEmpty()) throw new PedidoRecusado(FORMATO_INVALIDO);
            Map<String, Object> cab = cabecalho(p[0]);
            var chave = chaveiro.porKid(String.valueOf(cab.get("kid")))
                    .orElseThrow(() -> new PedidoRecusado(CHAVE_DESCONHECIDA));
            if (!chave.integrador().equals(integrador)) throw new PedidoRecusado(CHAVE_DE_OUTRO_INTEGRADOR);
            // a lista do que esta chave aceita é fixa: dir + A256GCM, e nada mais
            if (!"dir".equals(cab.get("alg")) || !"A256GCM".equals(cab.get("enc"))
                    || chave.algoritmo() != Chave.Algoritmo.AES_256_GCM)
                throw new PedidoRecusado(ALGORITMO_RECUSADO);
            // end::jwe-le[]
            if (!(cab.get("iat") instanceof Long iat)
                    || Duration.between(Instant.ofEpochSecond(iat), relogio.instant()).abs().compareTo(janela) > 0)
                throw new PedidoRecusado(FORA_DA_JANELA);
            byte[] claro = abrirCompacto(chave.segredo(), jwe);
            Map<String, Object> conteudo;
            try {
                conteudo = JsonPlano.ler(new String(claro, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw new PedidoRecusado(PIN_INVALIDO);
            } finally {
                java.util.Arrays.fill(claro, (byte) 0);
            }
            if (!cartaoDoPedido.equals(conteudo.get("cartao"))) throw new PedidoRecusado(CIFRA_INVALIDA);
            return DecifradorDePin.comoPin(String.valueOf(conteudo.get("pin")).getBytes(StandardCharsets.US_ASCII));
        }

        private static Map<String, Object> cabecalho(String parte) {
            try {
                return JsonPlano.ler(new String(decodificar(parte), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw new PedidoRecusado(FORMATO_INVALIDO);
            }
        }
    }

    private static byte[] decodificar(String parte) {
        try {
            return D64.decode(parte);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusado(FORMATO_INVALIDO);
        }
    }

    /** AES-256-GCM com a AAD que a RFC 7516 manda: os bytes ASCII do cabeçalho protegido já codificado. */
    private static byte[] gcm(int modo, SecretKey chave, byte[] iv, String protegido, byte[] dados) {
        try {
            var aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(modo, chave, new GCMParameterSpec(128, iv));
            aes.updateAAD(protegido.getBytes(StandardCharsets.US_ASCII));
            return aes.doFinal(dados);
        } catch (AEADBadTagException e) {
            throw new PedidoRecusado(CIFRA_INVALIDA);
        } catch (GeneralSecurityException e) {
            throw new PedidoRecusado(CIFRA_INVALIDA);
        }
    }
}
