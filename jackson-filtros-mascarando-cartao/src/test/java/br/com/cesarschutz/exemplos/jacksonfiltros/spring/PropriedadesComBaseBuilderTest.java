package br.com.cesarschutz.exemplos.jacksonfiltros.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.cesarschutz.exemplos.jacksonfiltros.Exemplos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seção 5 do post. Partindo de JsonMapper.builder(), as propriedades spring.jackson.* não valem para nenhum dos dois mappers.
 * A propriedade usada no teste liga a indentação do JSON.
 */
@SpringBootTest(classes = JacksonConfigTest.Aplicacao.class,
        properties = "spring.jackson.serialization.indent-output=true")
class PropriedadesComBaseBuilderTest {

    @Autowired
    private JsonMapper mapperPadrao;

    @Autowired
    @Qualifier("logMapper")
    private JsonMapper logMapper;

    @Test
    void aPropriedadeNaoVale() {
        assertFalse(mapperPadrao.writeValueAsString(Exemplos.pedido()).contains("\n"));
        assertFalse(logMapper.writeValueAsString(Exemplos.pedido()).contains("\n"));
    }
}
