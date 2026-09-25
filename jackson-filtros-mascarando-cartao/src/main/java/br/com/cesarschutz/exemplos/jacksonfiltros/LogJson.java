package br.com.cesarschutz.exemplos.jacksonfiltros;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LogJson {

    private final JsonMapper logMapper;

    public LogJson(@Qualifier("logMapper") JsonMapper logMapper) {
        this.logMapper = logMapper;
    }

    public String toJson(Object objeto) {
        try {
            return logMapper.writeValueAsString(objeto);
        } catch (JacksonException e) {
            // nunca cair no toString() aqui: ele não passa pelo filtro
            return "<falha ao serializar " + objeto.getClass().getSimpleName() + ">";
        }
    }
}
