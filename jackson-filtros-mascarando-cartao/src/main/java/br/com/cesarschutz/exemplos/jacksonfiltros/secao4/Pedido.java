package br.com.cesarschutz.exemplos.jacksonfiltros.secao4;

import java.util.List;
import java.util.Map;

public record Pedido(Long id, Cliente cliente, Cartao cartao,
                     List<Cartao> adicionais, Map<String, Object> extras) {}
