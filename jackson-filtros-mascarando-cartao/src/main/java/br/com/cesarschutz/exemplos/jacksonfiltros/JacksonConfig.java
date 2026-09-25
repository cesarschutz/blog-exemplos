package br.com.cesarschutz.exemplos.jacksonfiltros;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper objectMapper() {
        return baseBuilder().build();
    }

    @Bean
    public JsonMapper logMapper() {
        return baseBuilder()
                .addMixIn(Object.class, LogFilterMixin.class)
                .filterProvider(new SimpleFilterProvider()
                        .addFilter("logFilter", new MascaraCartaoFilter()))
                .build();
    }

    private JsonMapper.Builder baseBuilder() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
