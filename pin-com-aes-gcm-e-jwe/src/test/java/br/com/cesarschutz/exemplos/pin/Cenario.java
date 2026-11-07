package br.com.cesarschutz.exemplos.pin;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** O cenário dos testes: dois integradores, as chaves deles e um relógio parado. */
final class Cenario {

    static final String CARTAO = "4111111111111111"; // número de teste, não é de ninguém
    static final String OUTRO_CARTAO = "5555555555554444";
    static final Instant AGORA = Instant.parse("2026-11-07T12:00:00Z");
    static final Duration JANELA = Duration.ofSeconds(60);

    final SecureRandom sorteio = new SecureRandom();
    final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

    final Chave chaveA1 = chave("integrador-a-2026-11", "integrador-a");
    final Chave chaveA2 = chave("integrador-a-2027-05", "integrador-a");
    final Chave chaveB1 = chave("integrador-b-2026-11", "integrador-b");

    final Chaveiro.EmMemoria chaveiro = new Chaveiro.EmMemoria().guardar(chaveA1).guardar(chaveA2).guardar(chaveB1);

    final CifradorDePin cifrador = new CifradorDePin(sorteio, relogio);
    final DecifradorDePin decifrador = new DecifradorDePin(chaveiro, relogio, JANELA);

    Chave chave(String kid, String integrador) {
        byte[] bytes = new byte[32];
        sorteio.nextBytes(bytes);
        return new Chave(kid, integrador, Chave.Algoritmo.AES_256_GCM, new SecretKeySpec(bytes, "AES"));
    }

    /** O mesmo cifrador, com o relógio deslocado (o integrador com o relógio adiantado ou um pedido antigo). */
    CifradorDePin cifradorEm(Instant instante) {
        return new CifradorDePin(sorteio, Clock.fixed(instante, ZoneOffset.UTC));
    }
}
