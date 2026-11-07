package br.com.cesarschutz.exemplos.pin;

import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;

import static br.com.cesarschutz.exemplos.pin.PedidoRecusado.Motivo.*;
import static org.junit.jupiter.api.Assertions.*;

/** O JWE "dir" + "A256GCM": a ida e volta, a política do servidor e a construção conferida contra a RFC 7520. */
class JweTest {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();

    private final Cenario c = new Cenario();
    private final JweDePin jwe = new JweDePin(c.sorteio, c.relogio);
    private final JweDePin.Leitor leitor = new JweDePin.Leitor(c.chaveiro, c.relogio, Cenario.JANELA);

    @Test
    void oServidorLeOPinDoJwe() {
        String token = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO);
        assertArrayEquals("4321".toCharArray(), leitor.ler("integrador-a", Cenario.CARTAO, token));
    }

    @Test
    void oJweTemCincoPartesEASegundaVazia() {
        String[] partes = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO).split("\\.", -1);
        assertEquals(5, partes.length);
        assertEquals("", partes[1]); // "dir": nenhuma chave embrulhada
        assertEquals(12, D64.decode(partes[2]).length); // IV de 96 bits
        assertEquals(16, D64.decode(partes[4]).length); // etiqueta de 128 bits
    }

    @Test
    void oCabecalhoTrazOKidEOInstante() {
        String cabecalho = new String(D64.decode(jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO).split("\\.")[0]), StandardCharsets.UTF_8);
        assertEquals("{\"alg\":\"dir\",\"enc\":\"A256GCM\",\"kid\":\"integrador-a-2026-11\",\"iat\":" + Cenario.AGORA.getEpochSecond() + "}", cabecalho);
    }

    // tag::jwe-pede-outro-enc[]
    @Test
    void umJweQuePedeOutroEncERecusadoAntesDeQualquerCifra() {
        String token = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO);
        String trocado = trocarCabecalho(token, "enc", "A128GCM");
        var e = assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.CARTAO, trocado));
        assertEquals(ALGORITMO_RECUSADO, e.motivo());
    }
    // end::jwe-pede-outro-enc[]

    @Test
    void umJweComAlgNoneERecusado() {
        String trocado = trocarCabecalho(jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO), "alg", "none");
        assertEquals(ALGORITMO_RECUSADO, assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.CARTAO, trocado)).motivo());
    }

    @Test
    void oInstanteDoCabecalhoAlteradoQuebraAEtiqueta() {
        String token = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO);
        var cab = JsonPlano.ler(new String(D64.decode(token.split("\\.")[0]), StandardCharsets.UTF_8));
        cab.put("iat", Cenario.AGORA.getEpochSecond() - 5); // ainda dentro da janela
        String trocado = B64.encodeToString(JsonPlano.escrever(cab).getBytes(StandardCharsets.UTF_8)) + token.substring(token.indexOf('.'));
        assertEquals(CIFRA_INVALIDA, assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.CARTAO, trocado)).motivo());
    }

    @Test
    void oJweDeUmCartaoNaoServeNoPedidoDeOutro() {
        String token = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO);
        assertEquals(CIFRA_INVALIDA, assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.OUTRO_CARTAO, token)).motivo());
    }

    @Test
    void umJweComChaveEmbrulhadaNaoEDirERecusado() {
        String[] p = jwe.gerar(c.chaveA1, "4321", Cenario.CARTAO).split("\\.", -1);
        String comChave = p[0] + ".AAAA." + p[2] + "." + p[3] + "." + p[4];
        assertEquals(FORMATO_INVALIDO, assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.CARTAO, comChave)).motivo());
    }

    @Test
    void oKidDeOutroIntegradorERecusado() {
        String token = jwe.gerar(c.chaveB1, "4321", Cenario.CARTAO);
        assertEquals(CHAVE_DE_OUTRO_INTEGRADOR, assertThrows(PedidoRecusado.class, () -> leitor.ler("integrador-a", Cenario.CARTAO, token)).motivo());
    }

    /**
     * A construção é a da RFC: com a chave, o IV e o cabeçalho do exemplo da RFC 7520, seção 5.6 (Direct Encryption
     * Using AES-GCM, com A128GCM), sai o mesmo JWE, byte a byte, e o JWE da RFC abre no texto original. Os valores
     * vêm de github.com/ietf-jose/cookbook, jwe/5_6.direct_encryption_using_aes-gcm.json.
     */
    @Test
    void aConstrucaoBateComOExemploDaRfc7520() {
        var chave = new SecretKeySpec(D64.decode("XctOhJAkA-pD9Lh7ZgW_2A"), "AES");
        String protegido = "eyJhbGciOiJkaXIiLCJraWQiOiI3N2M3ZTJiOC02ZTEzLTQ1Y2YtODY3Mi02MTdiNWI0NTI0M2EiLCJlbmMiOiJBMTI4R0NNIn0";
        String texto = "You can trust us to stick with you through thick and thin–to the bitter end. And you can trust us to keep any secret of yours–closer than you keep it yourself. But you cannot trust us to let you face trouble alone, and go off without a word. We are your friends, Frodo.";
        String esperado = protegido + "..refa467QzzKx6QAB."
                + "JW_i_f52hww_ELQPGaYyeAB6HYGcR559l9TYnSovc23XJoBcW29rHP8yZOZG7YhLpT1bjFuvZPjQS-m0IFtVcXkZXdH_lr_FrdYt9HRUYkshtrMmIUAyGmUnd9zMDB2n0cRDIHAzFVeJUDxkUwVAE7_YGRPdcqMyiBoCO-FBdE-Nceb4h3-FtBP-c_BIwCPTjb9o0SbdcdREEMJMyZBH8ySWMVi1gPD9yxi-aQpGbSv_F9N4IZAxscj5g-NJsUPbjk29-s7LJAGb15wEBtXphVCgyy53CoIKLHHeJHXex45Uz9aKZSRSInZI-wjsY0yu3cT4_aQ3i1o-tiE-F8Ios61EKgyIQ4CWao8PFMj8TTnp"
                + ".vbb32Xvllea2OtmHAdccRQ";
        assertEquals(esperado, JweDePin.compactar(chave, protegido, D64.decode("refa467QzzKx6QAB"), texto.getBytes(StandardCharsets.UTF_8)));
        assertEquals(texto, new String(JweDePin.abrirCompacto(chave, esperado), StandardCharsets.UTF_8));
    }

    private static String trocarCabecalho(String token, String campo, String valor) {
        var cab = new LinkedHashMap<>(JsonPlano.ler(new String(D64.decode(token.split("\\.")[0]), StandardCharsets.UTF_8)));
        cab.put(campo, valor);
        return B64.encodeToString(JsonPlano.escrever(cab).getBytes(StandardCharsets.UTF_8)) + token.substring(token.indexOf('.'));
    }
}
