package br.com.cesarschutz.exemplos.jacksonfiltros.secao2;

import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("cartaoFilter")
public record Cartao(String titular, String numero, String cvv) {}
