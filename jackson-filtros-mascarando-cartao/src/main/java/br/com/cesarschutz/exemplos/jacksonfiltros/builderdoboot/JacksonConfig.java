package br.com.cesarschutz.exemplos.jacksonfiltros.builderdoboot;

import br.com.cesarschutz.exemplos.jacksonfiltros.LogFilterMixin;
import br.com.cesarschutz.exemplos.jacksonfiltros.MascaraCartaoFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Variação da seção 5 do post: os dois mappers partem do {@code JsonMapper.Builder}
 * do Spring Boot, então as propriedades {@code spring.jackson.*} continuam valendo.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper objectMapper(JsonMapper.Builder builder) {
        return builder.build();
    }

    @Bean
    public JsonMapper logMapper(JsonMapper.Builder builder) {
        return builder
                .addMixIn(Object.class, LogFilterMixin.class)
                .filterProvider(new SimpleFilterProvider()
                        .addFilter("logFilter", new MascaraCartaoFilter()))
                .build();
    }
}
