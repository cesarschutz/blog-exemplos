package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * "PostgreSQL fora do RDS", em repouso, opção 2: o pg_tde do Percona Server for PostgreSQL. O teste
 * roda o SQL do artigo, grava o mesmo número de cartão numa tabela tde_heap e numa tabela comum e
 * procura o número direto nos arquivos das duas, no disco do servidor.
 */
@DisplayName("Postgres: pg_tde no Percona Server for PostgreSQL")
class PgTdeTest {

    private static final DockerImageName PERCONA = DockerImageName.parse("percona/percona-distribution-postgresql:18");
    private static final String SENHA = "senha-de-teste";
    private static final String CARTAO = "4111111111111111";

    private static GenericContainer<?> percona;

    @BeforeAll
    static void subir() throws Exception {
        percona = new GenericContainer<>(PERCONA)
                .withEnv("POSTGRES_PASSWORD", SENHA)
                .withCommand("postgres", "-c", "shared_preload_libraries=pg_tde")
                .withExposedPorts(5432)
                // A imagem da Percona manda o log para arquivos (logging_collector): espera a porta abrir.
                .waitingFor(Wait.forListeningPort());
        percona.start();

        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(Files.readString(Path.of("postgres/pg_tde.sql")));
            comando.execute("CREATE TABLE cartao_comum (id bigint PRIMARY KEY, numero text NOT NULL)");
            comando.execute("INSERT INTO cartao VALUES (1, '" + CARTAO + "')");
            comando.execute("INSERT INTO cartao_comum VALUES (1, '" + CARTAO + "')");
            comando.execute("CHECKPOINT");
        }
    }

    @AfterAll
    static void descer() {
        percona.stop();
    }

    @Test
    @DisplayName("a tabela criada com USING tde_heap fica cifrada")
    void aTabelaFicaCifrada() throws Exception {
        assertEquals("t", consultar("SELECT pg_tde_is_encrypted('cartao')"));
        assertEquals("f", consultar("SELECT pg_tde_is_encrypted('cartao_comum')"));
    }

    @Test
    @DisplayName("no disco, o número aparece no arquivo da tabela comum e não no da tde_heap")
    void noDiscoSoATabelaComumMostraONumero() throws Exception {
        assertEquals(0, vezesNoArquivo("cartao"), "o arquivo da tabela tde_heap não deveria ter o número em claro");
        assertTrue(vezesNoArquivo("cartao_comum") > 0, "o arquivo da tabela comum deveria ter o número em claro");
    }

    @Test
    @DisplayName("quem tem usuário e senha continua lendo o número: a decifragem acontece por baixo")
    void quemTemCredencialLe() throws Exception {
        assertEquals(CARTAO, consultar("SELECT numero FROM cartao WHERE id = 1"));
    }

    private static int vezesNoArquivo(String tabela) throws Exception {
        String arquivo = consultar("SELECT current_setting('data_directory') || '/' || pg_relation_filepath('" + tabela + "')");
        ExecResult grep = percona.execInContainer("grep", "-c", CARTAO, arquivo);
        return Integer.parseInt(grep.getStdout().trim());
    }

    private static String consultar(String sql) throws SQLException {
        try (Connection conexao = conectar();
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }

    private static Connection conectar() throws SQLException {
        String url = "jdbc:postgresql://" + percona.getHost() + ":" + percona.getMappedPort(5432) + "/postgres";
        return DriverManager.getConnection(url, "postgres", SENHA);
    }
}
