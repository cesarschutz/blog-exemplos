package br.com.cesarschutz.exemplos.jacksonfiltros;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** O post diz que o código foi executado com Jackson 3.2.3; o build fixa essa versão. */
class VersaoJacksonTest {

    @Test
    void usaJackson323() {
        assertEquals("3.2.3", tools.jackson.databind.cfg.PackageVersion.VERSION.toString());
        assertEquals("3.2.3", tools.jackson.core.json.PackageVersion.VERSION.toString());
    }
}
