package br.com.cesarschutz.exemplos.jacksonfiltros.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cesarschutz.exemplos.jacksonfiltros.Exemplos;
import br.com.cesarschutz.exemplos.jacksonfiltros.LogJson;
import br.com.cesarschutz.exemplos.jacksonfiltros.builderdoboot.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seção 5 do post, variação com o JsonMapper.Builder do Spring Boot: o builder é criado
 * de novo a cada injeção, então o mixin do logMapper não vaza para o mapper da API.
 */
@SpringBootTest(classes = JacksonConfigBuilderDoBootTest.Aplicacao.class)
class JacksonConfigBuilderDoBootTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({JacksonConfig.class, LogJson.class})
    static class Aplicacao {
    }

    @Autowired
    private JsonMapper mapperPadrao;

    @Autowired
    @Qualifier("logMapper")
    private JsonMapper logMapper;

    @Autowired
    private LogJson logJson;

    @Test
    void oMixinNaoVazaParaOMapperDaApi() {
        assertEquals(Exemplos.PEDIDO_API, mapperPadrao.writeValueAsString(Exemplos.pedido()));
    }

    @Test
    void oLogMapperMascara() {
        assertEquals(Exemplos.PEDIDO_LOG, logMapper.writeValueAsString(Exemplos.pedido()));
        assertEquals(Exemplos.PEDIDO_LOG, logJson.toJson(Exemplos.pedido()));
    }
}
