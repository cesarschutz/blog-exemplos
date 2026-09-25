package br.com.cesarschutz.exemplos.jacksonfiltros.secao2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cesarschutz.exemplos.jacksonfiltros.MascaraCartaoFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Seção 3 do post: o MascaraCartaoFilter aplicado ao Cartao anotado com @JsonFilter. */
class MascarandoNumeroTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .filterProvider(new SimpleFilterProvider()
                    .addFilter("cartaoFilter", new MascaraCartaoFilter()))
            .build();

    @Test
    void mascaraONumeroERemoveOCvv() {
        String json = mapper.writeValueAsString(new Cartao("Cesar", "5502 0912 3456 7890", "123"));

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"550209******7890\"}", json);
    }

    @Test
    void numeroNuloSegueOFluxoNormal() {
        String json = mapper.writeValueAsString(new Cartao("Cesar", null, "123"));

        assertEquals("{\"titular\":\"Cesar\",\"numero\":null}", json);
    }
}
