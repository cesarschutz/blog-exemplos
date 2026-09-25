package br.com.cesarschutz.exemplos.jacksonfiltros.secao6;

import br.com.cesarschutz.exemplos.jacksonfiltros.NumeroCartao;

public record Cartao(String titular, @NumeroCartao String numero) {}
