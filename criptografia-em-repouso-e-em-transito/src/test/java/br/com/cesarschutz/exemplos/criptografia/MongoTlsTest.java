package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * "MongoDB fora do Atlas", em trânsito: o bloco net.tls do artigo num MongoDB Community, e o
 * cliente (o mongosh do próprio container) com a string de conexão do artigo.
 */
@DisplayName("MongoDB: requireTLS com o mongod.conf do artigo")
class MongoTlsTest {

    private static final DockerImageName MONGO = DockerImageName.parse("mongo:8.0");
    private static final String PING = "db.runCommand({ ping: 1 }).ok";
    private static final Certificados.Par CA = Certificados.ca("CA interna");
    private static final Certificados.Par SERVIDOR = Certificados.servidor(CA, "localhost");

    private static GenericContainer<?> mongo;

    @BeforeAll
    static void subir() throws Exception {
        mongo = mongoComTls(Files.readString(Path.of("mongo/mongod-tls.conf")));
        mongo.start();
    }

    @AfterAll
    static void descer() {
        mongo.stop();
    }

    @Test
    @DisplayName("sem TLS, o servidor recusa")
    void semTlsRecusa() throws Exception {
        ExecResult resultado = mongosh("mongodb://localhost:27017/?serverSelectionTimeoutMS=3000");
        assertNotEquals(0, resultado.getExitCode(), resultado.getStdout());
        assertTrue(mongo.getLogs().contains("only allow SSL connections"), "o log deveria explicar a recusa");
    }

    @Test
    @DisplayName("com tls=true e a CA (tlsCAFile), como na string do artigo, conecta")
    void comTlsEACaConecta() throws Exception {
        ExecResult resultado = mongosh("mongodb://localhost:27017/?tls=true&tlsCAFile=/etc/mongodb/tls/ca.pem");
        assertEquals(0, resultado.getExitCode(), resultado.getStderr());
        assertEquals("1", resultado.getStdout().trim());
    }

    @Test
    @DisplayName("com tls=true mas sem a CA, o cliente não confia no certificado e não conecta")
    void semACaNaoConecta() throws Exception {
        ExecResult resultado = mongosh("mongodb://localhost:27017/?tls=true&serverSelectionTimeoutMS=3000");
        assertNotEquals(0, resultado.getExitCode(), resultado.getStdout());
    }

    @Test
    @DisplayName("allowConnectionsWithoutCertificates: sem a linha, o cliente sem certificado é recusado")
    void semALinhaOClienteSemCertificadoERecusado() throws Exception {
        // As conexões dos testes acima não apresentam certificado de cliente e passam por causa dessa linha.
        String semALinha = Files.readString(Path.of("mongo/mongod-tls.conf"))
                .replaceAll("(?m)^\\s*allowConnectionsWithoutCertificates: true.*\\R", "");
        assertFalse(semALinha.contains("allowConnectionsWithoutCertificates"));
        try (GenericContainer<?> semPermissao = mongoComTls(semALinha)) {
            semPermissao.start();
            ExecResult resultado = semPermissao.execInContainer("mongosh", "--quiet",
                    "mongodb://localhost:27017/?tls=true&tlsCAFile=/etc/mongodb/tls/ca.pem&serverSelectionTimeoutMS=3000",
                    "--eval", PING);
            assertNotEquals(0, resultado.getExitCode(), resultado.getStdout());
            assertTrue(semPermissao.getLogs().contains("no SSL certificate provided by peer"), semPermissao.getLogs());
        }
    }

    private static GenericContainer<?> mongoComTls(String configuracao) {
        return new GenericContainer<>(MONGO)
                .withCopyToContainer(Transferable.of(configuracao), "/etc/mongod.conf")
                .withCopyToContainer(Transferable.of(SERVIDOR.certificadoEChavePem()), "/etc/mongodb/tls/mongod.pem")
                .withCopyToContainer(Transferable.of(CA.certificadoPem()), "/etc/mongodb/tls/ca.pem")
                .withCommand("--config", "/etc/mongod.conf")
                .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
    }

    private static ExecResult mongosh(String uri) throws Exception {
        return mongo.execInContainer("mongosh", "--quiet", uri, "--eval", PING);
    }
}
