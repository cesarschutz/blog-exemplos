package br.com.cesarschutz.exemplos.jacksonfiltros.secao4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cesarschutz.exemplos.jacksonfiltros.Exemplos;
import br.com.cesarschutz.exemplos.jacksonfiltros.LogFilterMixin;
import br.com.cesarschutz.exemplos.jacksonfiltros.MascaraCartaoFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Seção 4 do post: o mixin em Object aplica o filtro a todas as classes, só no mapper de log. */
class MixinEmObjectTest {

    @Test
    void mesmoPedidoPelosDoisMappers() {
        JsonMapper logMapper = JsonMapper.builder()
                .addMixIn(Object.class, LogFilterMixin.class)
                .filterProvider(new SimpleFilterProvider()
                        .addFilter("logFilter", new MascaraCartaoFilter()))
                .build();
        JsonMapper mapperNormal = JsonMapper.builder().build();

        assertEquals(Exemplos.PEDIDO_API, mapperNormal.writeValueAsString(Exemplos.pedido()));
        assertEquals(Exemplos.PEDIDO_LOG, logMapper.writeValueAsString(Exemplos.pedido()));
    }
}
