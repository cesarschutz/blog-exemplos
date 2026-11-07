package br.com.cesarschutz.exemplos.pin;

/**
 * O pedido não passou numa das conferências do servidor. O motivo é para o log e para os testes; a resposta ao
 * integrador deve ser a mesma para todos os motivos de cifra, para não ensinar a quem tenta adivinhar o que falhou.
 */
public final class PedidoRecusado extends RuntimeException {

    public enum Motivo {
        /** O kid não existe no chaveiro (chave desconhecida ou já aposentada). */
        CHAVE_DESCONHECIDA,
        /** A chave existe, mas é de outro integrador. */
        CHAVE_DE_OUTRO_INTEGRADOR,
        /** O cliente pediu um algoritmo diferente do que a chave aceita. */
        ALGORITMO_RECUSADO,
        /** O instante está fora da janela aceita (pedido velho, repetido ou com relógio errado). */
        FORA_DA_JANELA,
        /** A etiqueta não confere: a cifra, o IV, o cartão, o instante ou o kid foram alterados, ou a chave é outra. */
        CIFRA_INVALIDA,
        /** Decifrou, mas o conteúdo não é um PIN. */
        PIN_INVALIDO,
        /** O formato do pedido está errado (IV do tamanho errado, partes faltando). */
        FORMATO_INVALIDO
    }

    private static final long serialVersionUID = 1L;

    private final Motivo motivo;

    public PedidoRecusado(Motivo motivo) {
        super(motivo.name());
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
