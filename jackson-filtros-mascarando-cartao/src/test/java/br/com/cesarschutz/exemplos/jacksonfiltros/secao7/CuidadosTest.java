package br.com.cesarschutz.exemplos.jacksonfiltros.secao7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cesarschutz.exemplos.jacksonfiltros.LogFilterMixin;
import br.com.cesarschutz.exemplos.jacksonfiltros.MascaraCartaoFilter;
import br.com.cesarschutz.exemplos.jacksonfiltros.secao4.Cartao;
import com.fasterxml.jackson.annotation.JsonFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Seção 7 do post: o que o filtro cobre e o que não cobre. */
class CuidadosTest {

    @JsonFilter("outroId")
    record ComFiltroProprio(String titular, String numero) {}

    record CartaoNumerico(String titular, Long numero) {}

    private static JsonMapper logMapper(SimpleFilterProvider provider) {
        return JsonMapper.builder()
                .addMixIn(Object.class, LogFilterMixin.class)
                .filterProvider(provider)
                .build();
    }

    private final JsonMapper logMapper = logMapper(new SimpleFilterProvider()
            .addFilter("logFilter", new MascaraCartaoFilter()));

    @Test
    void toStringDoRecordImprimeTudo() {
        assertEquals("Cartao[titular=Cesar, numero=5502091234567890, cvv=123]",
                new Cartao("Cesar", "5502091234567890", "123").toString());
    }

    @Test
    void stringSoltaNaoPassaPeloFiltro() {
        assertEquals("\"5502091234567890\"", logMapper.writeValueAsString("5502091234567890"));
    }

    @Test
    void filtroProprioNaClasseTemPrioridadeSobreOMixin() {
        Exception erro = assertThrows(Exception.class,
                () -> logMapper.writeValueAsString(new ComFiltroProprio("Cesar", "5502091234567890")));

        assertTrue(erro.getMessage().contains("No filter configured with id 'outroId'"), erro.getMessage());
    }

    @Test
    void filtroPadraoCobreIdsSemRegistro() {
        JsonMapper comPadrao = logMapper(new SimpleFilterProvider()
                .addFilter("logFilter", new MascaraCartaoFilter())
                .setDefaultFilter(new MascaraCartaoFilter()));

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"550209******7890\"}",
                comPadrao.writeValueAsString(new ComFiltroProprio("Cesar", "5502091234567890")));
    }

    @Test
    void leituraNaoPassaPeloFiltro() {
        Cartao lido = logMapper.readValue(
                "{\"titular\":\"Cesar\",\"numero\":\"5502091234567890\",\"cvv\":\"123\"}", Cartao.class);

        assertEquals(new Cartao("Cesar", "5502091234567890", "123"), lido);
    }

    @Test
    void campoMascaradoSaiComoString() {
        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"550209******7890\"}",
                logMapper.writeValueAsString(new CartaoNumerico("Cesar", 5502091234567890L)));
    }
}
