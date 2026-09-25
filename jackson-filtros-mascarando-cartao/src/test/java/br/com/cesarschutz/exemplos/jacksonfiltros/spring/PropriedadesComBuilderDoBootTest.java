package br.com.cesarschutz.exemplos.jacksonfiltros.spring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cesarschutz.exemplos.jacksonfiltros.Exemplos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seção 5 do post. Partindo do JsonMapper.Builder do Spring Boot, as propriedades spring.jackson.* valem para os dois mappers.
 * A propriedade usada no teste liga a indentação do JSON.
 */
@SpringBootTest(classes = JacksonConfigBuilderDoBootTest.Aplicacao.class,
        properties = "spring.jackson.serialization.indent-output=true")
class PropriedadesComBuilderDoBootTest {

    @Autowired
    private JsonMapper mapperPadrao;

    @Autowired
    @Qualifier("logMapper")
    private JsonMapper logMapper;

    @Test
    void aPropriedadeVale() {
        assertTrue(mapperPadrao.writeValueAsString(Exemplos.pedido()).contains("\n"));
        assertTrue(logMapper.writeValueAsString(Exemplos.pedido()).contains("\n"));
    }
}
