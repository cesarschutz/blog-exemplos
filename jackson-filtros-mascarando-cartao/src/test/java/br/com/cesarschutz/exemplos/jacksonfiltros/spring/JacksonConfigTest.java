package br.com.cesarschutz.exemplos.jacksonfiltros.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cesarschutz.exemplos.jacksonfiltros.Exemplos;
import br.com.cesarschutz.exemplos.jacksonfiltros.JacksonConfig;
import br.com.cesarschutz.exemplos.jacksonfiltros.LogJson;
import br.com.cesarschutz.exemplos.jacksonfiltros.secao4.Pedido;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/** Seção 5 do post: os dois mappers no Spring, com o JacksonConfig que usa baseBuilder(). */
@SpringBootTest(classes = JacksonConfigTest.Aplicacao.class)
class JacksonConfigTest {

    private static final Logger log = LoggerFactory.getLogger(JacksonConfigTest.class);

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
    void oMapperPrimaryDevolveTudo() {
        assertEquals(Exemplos.PEDIDO_API, mapperPadrao.writeValueAsString(Exemplos.pedido()));
    }

    @Test
    void oLogMapperMascara() {
        assertEquals(Exemplos.PEDIDO_LOG, logMapper.writeValueAsString(Exemplos.pedido()));
    }

    @Test
    void logJsonUsaOMapperDeLog() {
        Pedido pedido = Exemplos.pedido();

        log.info("Pedido recebido: {}", logJson.toJson(pedido));

        assertEquals(Exemplos.PEDIDO_LOG, logJson.toJson(pedido));
    }
}
