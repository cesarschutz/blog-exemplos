package br.com.cesarschutz.exemplos.pin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Onde o servidor busca a chave pelo {@code kid}.
 *
 * <p>Em produção, a chave não fica no banco nem na configuração: fica guardada cifrada por uma chave de um serviço
 * de gerenciamento de chaves (um KMS) e só é decifrada na memória do servidor, quando ele sobe ou na primeira vez
 * que precisa dela. O exemplo guarda em memória para os testes rodarem sem conta em serviço nenhum; a interface é o
 * que o resto do código conhece.
 */
public interface Chaveiro {

    Optional<Chave> porKid(String kid);

    /** O chaveiro em memória dos testes. */
    final class EmMemoria implements Chaveiro {
        private final Map<String, Chave> chaves = new ConcurrentHashMap<>();

        public EmMemoria guardar(Chave chave) {
            chaves.put(chave.kid(), chave);
            return this;
        }

        public void remover(String kid) {
            chaves.remove(kid);
        }

        @Override
        public Optional<Chave> porKid(String kid) {
            return Optional.ofNullable(chaves.get(kid));
        }
    }
}
