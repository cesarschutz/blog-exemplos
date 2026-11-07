package br.com.cesarschutz.exemplos.pin;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;

import static br.com.cesarschutz.exemplos.pin.PedidoRecusado.Motivo.*;
import static org.junit.jupiter.api.Assertions.*;

/** O que o capítulo 7 afirma sobre o desenho com AES-256-GCM, um teste por afirmação. */
class AesGcmTest {

    private final Cenario c = new Cenario();

    @Test
    void oServidorDecifraOPinQueOIntegradorCifrou() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        assertArrayEquals("4321".toCharArray(), c.decifrador.decifrar("integrador-a", pedido));
    }

    @Test
    void oMesmoPinNoMesmoCartaoSaiDiferenteACadaPedido() {
        var um = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var outro = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        assertFalse(Arrays.equals(um.cifrado(), outro.cifrado()));
    }

    @Test
    void nenhumIvSeRepeteEmDezMilPedidos() {
        var vistos = new HashSet<String>();
        for (int i = 0; i < 10_000; i++)
            assertTrue(vistos.add(Arrays.toString(c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO).iv())));
        assertEquals(10_000, vistos.size());
    }

    @Test
    void umBitTrocadoNaCifraERecusado() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        byte[] adulterado = pedido.cifrado().clone();
        adulterado[0] ^= 1;
        assertMotivo(CIFRA_INVALIDA, troca(pedido, p -> new PedidoDeValidacao(p.kid(), p.cartao(), p.instante(), p.iv(), adulterado, null)));
    }

    // tag::cartao-trocado[]
    @Test
    void oPinCifradoParaUmCartaoNaoServeParaOutro() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var comOutroCartao = new PedidoDeValidacao(pedido.kid(), Cenario.OUTRO_CARTAO,
                pedido.instante(), pedido.iv(), pedido.cifrado(), null);
        assertMotivo(CIFRA_INVALIDA, comOutroCartao); // a AAD não confere: a etiqueta recusa
    }
    // end::cartao-trocado[]

    @Test
    void oInstanteTrocadoNoCaminhoERecusado() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var adiantado = new PedidoDeValidacao(pedido.kid(), pedido.cartao(), pedido.instante().plusSeconds(10),
                pedido.iv(), pedido.cifrado(), null);
        assertMotivo(CIFRA_INVALIDA, adiantado);
    }

    @Test
    void oPedidoForaDaJanelaERecusadoSemTentarDecifrar() {
        var velho = c.cifradorEm(Cenario.AGORA.minusSeconds(120)).cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        assertMotivo(FORA_DA_JANELA, velho);
    }

    @Test
    void aChaveDeUmIntegradorNaoValeParaOutro() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var e = assertThrows(PedidoRecusado.class, () -> c.decifrador.decifrar("integrador-b", pedido));
        assertEquals(CHAVE_DE_OUTRO_INTEGRADOR, e.motivo());
    }

    @Test
    void oKidTrocadoParaOutraChaveDoMesmoIntegradorERecusado() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        assertMotivo(CIFRA_INVALIDA, troca(pedido, p -> new PedidoDeValidacao(c.chaveA2.kid(), p.cartao(), p.instante(), p.iv(), p.cifrado(), null)));
    }

    // tag::algoritmo-do-cliente[]
    @Test
    void oServidorNaoAceitaOAlgoritmoQueOClientePede() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var pedindoOutro = new PedidoDeValidacao(pedido.kid(), pedido.cartao(), pedido.instante(),
                pedido.iv(), pedido.cifrado(), "AES_128_CBC");
        assertMotivo(ALGORITMO_RECUSADO, pedindoOutro);
    }
    // end::algoritmo-do-cliente[]

    @Test
    void naRotacaoAsDuasChavesValemEAAposentadaDeixaDeValer() {
        var antigo = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        var novo = c.cifrador.cifrar(c.chaveA2, "4321", Cenario.CARTAO);
        assertArrayEquals("4321".toCharArray(), c.decifrador.decifrar("integrador-a", antigo));
        assertArrayEquals("4321".toCharArray(), c.decifrador.decifrar("integrador-a", novo));
        c.chaveiro.remover(c.chaveA1.kid());
        assertMotivo(CHAVE_DESCONHECIDA, antigo);
        assertArrayEquals("4321".toCharArray(), c.decifrador.decifrar("integrador-a", novo));
    }

    @Test
    void decifrarAlgoQueNaoEPinERecusado() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "12ab", Cenario.CARTAO);
        assertMotivo(PIN_INVALIDO, pedido);
    }

    @Test
    void ivDoTamanhoErradoERecusado() {
        var pedido = c.cifrador.cifrar(c.chaveA1, "4321", Cenario.CARTAO);
        assertMotivo(FORMATO_INVALIDO, troca(pedido, p -> new PedidoDeValidacao(p.kid(), p.cartao(), p.instante(), new byte[8], p.cifrado(), null)));
    }

    private static PedidoDeValidacao troca(PedidoDeValidacao p, Function<PedidoDeValidacao, PedidoDeValidacao> f) {
        return f.apply(p);
    }

    private void assertMotivo(PedidoRecusado.Motivo esperado, PedidoDeValidacao pedido) {
        var e = assertThrows(PedidoRecusado.class, () -> c.decifrador.decifrar("integrador-a", pedido));
        assertEquals(esperado, e.motivo());
    }
}
