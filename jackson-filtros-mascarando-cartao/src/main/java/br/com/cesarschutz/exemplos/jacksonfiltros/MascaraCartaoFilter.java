package br.com.cesarschutz.exemplos.jacksonfiltros;

import java.util.Set;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.jdk.MapProperty;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

public class MascaraCartaoFilter extends SimpleBeanPropertyFilter {

    private static final Set<String> CAMPOS_MASCARADOS = Set.of("numero", "numeroCartao", "pan");
    private static final Set<String> CAMPOS_REMOVIDOS = Set.of("cvv", "senha");

    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator g, SerializationContext ctxt,
                                    PropertyWriter writer) throws Exception {
        String nome = writer.getName();

        // 1. Campo proibido: não escreve nada
        if (CAMPOS_REMOVIDOS.contains(nome)) {
            writer.serializeAsOmittedProperty(pojo, g, ctxt);
            return;
        }

        // 2. Campo sensível: escreve a versão mascarada
        if (CAMPOS_MASCARADOS.contains(nome)) {
            Object valor = valorDe(pojo, writer);
            if (valor != null) {
                g.writeStringProperty(nome, mascarar(valor.toString()));
                return;
            }
        }

        // 3. Qualquer outro campo: segue o fluxo normal
        writer.serializeAsProperty(pojo, g, ctxt);
    }

    private static Object valorDe(Object pojo, PropertyWriter writer) throws Exception {
        if (writer instanceof BeanPropertyWriter propriedade) {
            return propriedade.get(pojo);          // campo de um objeto/record
        }
        if (writer instanceof MapProperty entrada) {
            return entrada.getValue();             // entrada de um Map
        }
        return null;
    }

    static String mascarar(String numero) {
        String digitos = numero.replaceAll("\\D", "");
        if (digitos.length() < 12) {
            return "*".repeat(digitos.length());
        }
        return digitos.substring(0, 6)
                + "*".repeat(digitos.length() - 10)
                + digitos.substring(digitos.length() - 4);
    }
}
