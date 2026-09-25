package br.com.cesarschutz.exemplos.jacksonfiltros.secao2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Seção 2 do post: removendo o CVV com um filtro pronto. */
class RemovendoCvvTest {

    @Test
    void serializeAllExceptRemoveOCvv() {
        JsonMapper mapper = JsonMapper.builder()
                .filterProvider(new SimpleFilterProvider()
                        .addFilter("cartaoFilter", SimpleBeanPropertyFilter.serializeAllExcept("cvv")))
                .build();

        String json = mapper.writeValueAsString(new Cartao("Cesar", "5502091234567890", "123"));

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"5502091234567890\"}", json);
    }

    @Test
    void providerPassadoSoNaHoraDeEscrever() {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("cartaoFilter", SimpleBeanPropertyFilter.serializeAllExcept("cvv"));
        JsonMapper mapper = JsonMapper.builder().build();
        Cartao objeto = new Cartao("Cesar", "5502091234567890", "123");

        String json = mapper.writer(filterProvider).writeValueAsString(objeto);

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"5502091234567890\"}", json);
    }

    @Test
    void filtrosProntos() {
        Cartao cartao = new Cartao("Cesar", "5502091234567890", "123");

        assertEquals("{\"titular\":\"Cesar\",\"numero\":\"5502091234567890\",\"cvv\":\"123\"}",
                comFiltro(SimpleBeanPropertyFilter.serializeAll()).writeValueAsString(cartao));
        assertEquals("{\"titular\":\"Cesar\"}",
                comFiltro(SimpleBeanPropertyFilter.filterOutAllExcept("titular")).writeValueAsString(cartao));
        assertEquals("{}",
                comFiltro(SimpleBeanPropertyFilter.filterOutAll()).writeValueAsString(cartao));
    }

    @Test
    void mapperSemProviderFalha() {
        JsonMapper semProvider = JsonMapper.builder().build();

        InvalidDefinitionException erro = assertThrows(InvalidDefinitionException.class,
                () -> semProvider.writeValueAsString(new Cartao("Cesar", "5502091234567890", "123")));

        assertTrue(erro.getMessage().contains(
                "Cannot resolve PropertyFilter with id 'cartaoFilter'; no FilterProvider configured"),
                erro.getMessage());
    }

    private static JsonMapper comFiltro(SimpleBeanPropertyFilter filtro) {
        return JsonMapper.builder()
                .filterProvider(new SimpleFilterProvider().addFilter("cartaoFilter", filtro))
                .build();
    }
}
