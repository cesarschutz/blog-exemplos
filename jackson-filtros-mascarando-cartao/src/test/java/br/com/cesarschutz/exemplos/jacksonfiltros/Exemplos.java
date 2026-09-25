package br.com.cesarschutz.exemplos.jacksonfiltros;

import br.com.cesarschutz.exemplos.jacksonfiltros.secao4.Cartao;
import br.com.cesarschutz.exemplos.jacksonfiltros.secao4.Cliente;
import br.com.cesarschutz.exemplos.jacksonfiltros.secao4.Pedido;
import java.util.List;
import java.util.Map;

/** O pedido da seção 4 do post e as duas saídas mostradas lá, em uma linha só. */
public final class Exemplos {

    public static final String PEDIDO_API =
            "{\"id\":10,\"cliente\":{\"nome\":\"Cesar\",\"cpf\":\"123.456.789-00\"},"
            + "\"cartao\":{\"titular\":\"Cesar\",\"numero\":\"5502091234567890\",\"cvv\":\"123\"},"
            + "\"adicionais\":[{\"titular\":\"Maria\",\"numero\":\"4000123412341234\",\"cvv\":\"999\"}],"
            + "\"extras\":{\"numero\":\"4111111111111111\"}}";

    public static final String PEDIDO_LOG =
            "{\"id\":10,\"cliente\":{\"nome\":\"Cesar\",\"cpf\":\"123.456.789-00\"},"
            + "\"cartao\":{\"titular\":\"Cesar\",\"numero\":\"550209******7890\"},"
            + "\"adicionais\":[{\"titular\":\"Maria\",\"numero\":\"400012******1234\"}],"
            + "\"extras\":{\"numero\":\"411111******1111\"}}";

    private Exemplos() {
    }

    public static Pedido pedido() {
        return new Pedido(10L,
                new Cliente("Cesar", "123.456.789-00"),
                new Cartao("Cesar", "5502091234567890", "123"),
                List.of(new Cartao("Maria", "4000123412341234", "999")),
                Map.of("numero", "4111111111111111"));
    }
}
