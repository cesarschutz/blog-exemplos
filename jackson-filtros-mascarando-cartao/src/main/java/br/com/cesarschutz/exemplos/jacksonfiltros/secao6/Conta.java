package br.com.cesarschutz.exemplos.jacksonfiltros.secao6;

import br.com.cesarschutz.exemplos.jacksonfiltros.NumeroCartao;

public class Conta {
    @NumeroCartao
    private String cartaoVinculado;
    private String numero;          // número da conta: não deve ser mascarado

    // o post mostra só "// getters..."; aqui estão o construtor e os getters
    public Conta(String cartaoVinculado, String numero) {
        this.cartaoVinculado = cartaoVinculado;
        this.numero = numero;
    }

    public String getCartaoVinculado() {
        return cartaoVinculado;
    }

    public String getNumero() {
        return numero;
    }
}
