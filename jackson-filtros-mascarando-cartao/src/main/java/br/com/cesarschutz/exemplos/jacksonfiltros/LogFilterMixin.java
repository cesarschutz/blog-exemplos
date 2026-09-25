package br.com.cesarschutz.exemplos.jacksonfiltros;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("logFilter")
public abstract class LogFilterMixin {
}
