package br.com.cesarschutz.exemplos.jacksonfiltros;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

public class MascaraPorAnotacaoFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator g, SerializationContext ctxt,
                                    PropertyWriter writer) throws Exception {
        if (writer.getAnnotation(NumeroCartao.class) != null
                && writer instanceof BeanPropertyWriter propriedade) {
            Object valor = propriedade.get(pojo);
            if (valor != null) {
                g.writeStringProperty(writer.getName(), MascaraCartaoFilter.mascarar(valor.toString()));
                return;
            }
        }
        writer.serializeAsProperty(pojo, g, ctxt);
    }
}
