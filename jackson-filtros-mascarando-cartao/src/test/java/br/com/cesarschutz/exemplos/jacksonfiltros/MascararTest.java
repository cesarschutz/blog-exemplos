package br.com.cesarschutz.exemplos.jacksonfiltros;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A função mascarar() da seção 3. */
class MascararTest {

    @Test
    void mantemOsSeisPrimeirosEOsQuatroUltimos() {
        assertEquals("550209******7890", MascaraCartaoFilter.mascarar("5502091234567890"));
    }

    @Test
    void ignoraEspacosEHifens() {
        assertEquals("550209******7890", MascaraCartaoFilter.mascarar("5502 0912 3456 7890"));
        assertEquals("550209******7890", MascaraCartaoFilter.mascarar("5502-0912-3456-7890"));
    }

    @Test
    void valorCurtoDemaisParaSerCartaoFicaTodoMascarado() {
        assertEquals("*******", MascaraCartaoFilter.mascarar("1234567"));
    }
}
