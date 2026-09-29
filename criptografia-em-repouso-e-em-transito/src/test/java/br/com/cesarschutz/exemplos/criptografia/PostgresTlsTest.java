package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.SslMode;
import org.postgresql.util.PSQLException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * "PostgreSQL fora do RDS", em trânsito: o postgresql.conf e o pg_hba.conf do artigo num Postgres de
 * verdade, e o cliente JDBC com cada sslmode. Um segundo servidor faz o papel de "alguém no meio do
 * caminho": responde com um certificado de outra CA, a impostora.
 */
@DisplayName("Postgres: TLS com o postgresql.conf e o pg_hba.conf do artigo")
class PostgresTlsTest {

    private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:17");
    private static final String SENHA = "senha-de-teste";

    private static final Certificados.Par CA_DO_BANCO = Certificados.ca("CA do banco");
    private static final Certificados.Par CA_IMPOSTORA = Certificados.ca("CA impostora");

    private static GenericContainer<?> banco;
    private static GenericContainer<?> impostor;
    private static Path caDoBanco;

    @BeforeAll
    static void subir() {
        banco = postgresComTls(Certificados.servidor(CA_DO_BANCO, "localhost"));
        impostor = postgresComTls(Certificados.servidor(CA_IMPOSTORA, "localhost"));
        Startables.deepStart(banco, impostor).join();
        caDoBanco = CA_DO_BANCO.emArquivo();
    }

    @AfterAll
    static void descer() {
        banco.stop();
        impostor.stop();
    }

    @Test
    @DisplayName("sem TLS, o pg_hba.conf recusa a conexão (hostnossl ... reject)")
    void semTlsORecusa() {
        PSQLException erro = assertThrows(PSQLException.class, () -> conectar(banco, "localhost", "sslmode=disable"));
        assertTrue(erro.getMessage().contains("no encryption"), erro.getMessage());
    }

    @Test
    @DisplayName("o padrão do driver JDBC é sslmode=prefer, que tenta TLS e não valida o certificado")
    void oPadraoEPrefer() throws Exception {
        assertEquals(SslMode.PREFER, SslMode.of(new Properties()));
        try (Connection conexao = conectar(banco, "localhost", "")) {
            assertEquals("TLSv1.3", versaoDoTls(conexao));
        }
        // Mesmo com o servidor impostor, o padrão conecta: prefer não confere o certificado.
        try (Connection conexao = conectar(impostor, "localhost", "")) {
            assertEquals("TLSv1.3", versaoDoTls(conexao));
        }
    }

    @Test
    @DisplayName("verify-full com a CA do banco conecta no banco de verdade")
    void verifyFullConecta() throws Exception {
        try (Connection conexao = conectar(banco, "localhost", "sslmode=verify-full&sslrootcert=" + caDoBanco)) {
            assertEquals("TLSv1.3", versaoDoTls(conexao));
        }
    }

    @Test
    @DisplayName("require aceita o impostor: cifra, mas não confere quem está do outro lado")
    void requireAceitaOImpostor() throws Exception {
        try (Connection conexao = conectar(impostor, "localhost", "sslmode=require&sslrootcert=" + caDoBanco)) {
            assertEquals("TLSv1.3", versaoDoTls(conexao));
        }
    }

    @Test
    @DisplayName("verify-full recusa o impostor: o certificado dele não é da CA do banco")
    void verifyFullRecusaOImpostor() {
        PSQLException erro = assertThrows(PSQLException.class,
                () -> conectar(impostor, "localhost", "sslmode=verify-full&sslrootcert=" + caDoBanco));
        assertTrue(erro.getMessage().contains("SSL"), erro.getMessage());
    }

    @Test
    @DisplayName("verify-full também confere o nome: o certificado é de localhost, não de 127.0.0.1")
    void verifyFullConfereONome() {
        PSQLException erro = assertThrows(PSQLException.class,
                () -> conectar(banco, "127.0.0.1", "sslmode=verify-full&sslrootcert=" + caDoBanco));
        assertTrue(erro.getMessage().contains("127.0.0.1"), erro.getMessage());
    }

    @Test
    @DisplayName("a consulta do artigo mostra quem está conectado com TLS")
    void quemUsaTls() throws Exception {
        String consulta = Files.readString(Path.of("postgres/quem-usa-tls.sql"));
        List<String> linhas = new ArrayList<>();
        try (Connection conexao = conectar(banco, "localhost", "sslmode=verify-full&sslrootcert=" + caDoBanco);
                Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery(consulta)) {
            while (resultado.next()) {
                linhas.add(resultado.getString("usename") + " | " + resultado.getString("client_addr") + " | "
                        + resultado.getBoolean("ssl") + " | " + resultado.getString("version"));
            }
        }
        linhas.forEach(System.out::println);
        assertTrue(linhas.stream().anyMatch(linha -> linha.startsWith("postgres | ") && linha.endsWith("| true | TLSv1.3")),
                linhas.toString());
    }

    @Test
    @DisplayName("pgcrypto: a chave passa pelo servidor na query e aparece no log")
    void pgcryptoDeixaAChaveNoLog() throws Exception {
        try (Connection conexao = conectar(banco, "localhost", "sslmode=verify-full&sslrootcert=" + caDoBanco);
                Statement comando = conexao.createStatement()) {
            comando.execute("ALTER SYSTEM SET log_statement = 'all'");
            comando.execute("SELECT pg_reload_conf()");
        }
        // Uma conexão nova, já com o log de todos os comandos ligado.
        try (Connection conexao = conectar(banco, "localhost", "sslmode=verify-full&sslrootcert=" + caDoBanco);
                Statement comando = conexao.createStatement()) {
            comando.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            try (ResultSet cifrado = comando.executeQuery(
                    "SELECT pgp_sym_encrypt('4111111111111111', 'minha-chave-secreta')")) {
                assertTrue(cifrado.next());
            }
        }
        String log = banco.getLogs();
        assertTrue(log.contains("minha-chave-secreta"), "a chave deveria aparecer no log do servidor");
        assertTrue(log.contains("4111111111111111"), "o valor aberto deveria aparecer no log do servidor");
    }

    /**
     * O Postgres oficial com o bloco do artigo. O que não está no artigo é só o encanamento do
     * container: os certificados entram com dono postgres e a chave com 0600 (senão o servidor se
     * recusa a usá-la), o postgresql.conf do container inclui o do artigo e o pg_hba.conf é trocado.
     */
    private static GenericContainer<?> postgresComTls(Certificados.Par certificado) {
        String ligarOArtigo = """
                echo "include '/etc/postgresql/postgresql.conf'" >> "$PGDATA/postgresql.conf"
                cp /etc/postgresql/pg_hba.conf "$PGDATA/pg_hba.conf"
                """;
        return new GenericContainer<>(POSTGRES)
                .withEnv("POSTGRES_PASSWORD", SENHA)
                .withCopyToContainer(Transferable.of(certificado.certificadoPem()), "/tls/server.crt")
                .withCopyToContainer(Transferable.of(certificado.chavePem()), "/tls/server.key")
                .withCopyToContainer(MountableFile.forHostPath("postgres/postgresql.conf"), "/etc/postgresql/postgresql.conf")
                .withCopyToContainer(MountableFile.forHostPath("postgres/pg_hba.conf"), "/etc/postgresql/pg_hba.conf")
                .withCopyToContainer(Transferable.of(ligarOArtigo), "/docker-entrypoint-initdb.d/tls.sh")
                .withCommand("sh", "-c", """
                        install -d -o postgres /etc/postgresql/tls \
                        && install -o postgres -m 644 /tls/server.crt /etc/postgresql/tls/server.crt \
                        && install -o postgres -m 600 /tls/server.key /etc/postgresql/tls/server.key \
                        && exec docker-entrypoint.sh postgres""")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    }

    private static Connection conectar(GenericContainer<?> servidor, String host, String parametros) throws SQLException {
        String url = "jdbc:postgresql://" + host + ":" + servidor.getMappedPort(5432) + "/postgres"
                + (parametros.isEmpty() ? "" : "?" + parametros);
        return DriverManager.getConnection(url, "postgres", SENHA);
    }

    private static String versaoDoTls(Connection conexao) throws SQLException {
        try (Statement comando = conexao.createStatement();
                ResultSet resultado = comando.executeQuery("SELECT ssl, version FROM pg_stat_ssl WHERE pid = pg_backend_pid()")) {
            assertTrue(resultado.next());
            assertTrue(resultado.getBoolean("ssl"));
            String versao = resultado.getString("version");
            assertFalse(resultado.next());
            return versao;
        }
    }
}
