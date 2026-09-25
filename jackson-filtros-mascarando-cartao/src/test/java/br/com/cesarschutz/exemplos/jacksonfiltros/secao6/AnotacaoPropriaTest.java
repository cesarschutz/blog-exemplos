package br.com.cesarschutz.exemplos.jacksonfiltros.secao6;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cesarschutz.exemplos.jacksonfiltros.LogFilterMixin;
import br.com.cesarschutz.exemplos.jacksonfiltros.MascaraPorAnotacaoFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Seção 6 do post: o filtro procura a anotação @NumeroCartao em vez do nome do campo. */
class AnotacaoPropriaTest {

    private final JsonMapper logMapper = JsonMapper.builder()
            .addMixIn(Object.class, LogFilterMixin.class)
            .filterProvider(new SimpleFilterProvider()
                    .addFilter("logFilter", new MascaraPorAnotacaoFilter()))
            .build();

    @Test
    void mascaraSoOCampoAnotado() {
        String json = logMapper.writeValueAsString(new Conta("5502091234567890", "12345-6"));

        assertEquals("{\"cartaoVinculado\":\"550209******7890\",\"numero\":\"12345-6\"}", json);
    }

    @Test
    void funcionaEmRecords() {
        String json = logMapper.writeValueAsString(new Cartao("Cesar", "5502091234567890"));

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"550209******7890\"}", json);
    }

    @Test
    void outrosMappersIgnoramAAnotacao() {
        JsonMapper mapperNormal = JsonMapper.builder().build();

        assertEquals("{\"cartaoVinculado\":\"5502091234567890\",\"numero\":\"12345-6\"}",
                mapperNormal.writeValueAsString(new Conta("5502091234567890", "12345-6")));
    }
}
