package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * "E a criptografia em uso?": o Queryable Encryption do MongoDB. A aplicação (um mongosh com a chave)
 * cifra o CPF antes de enviar e consulta por igualdade; o DBA (um mongosh sem a chave) só vê o CPF
 * cifrado, no BinData de subtipo 6. A cifragem automática exige MongoDB Enterprise (ou Atlas) 7.0 ou
 * mais novo, num replica set.
 */
@DisplayName("MongoDB: Queryable Encryption, a aplicação e o DBA")
class QueryableEncryptionTest {

    private static final DockerImageName ENTERPRISE = DockerImageName.parse("mongodb/mongodb-enterprise-server:8.0-ubuntu2204");
    private static final String REPLICA_SET = "mongodb://localhost:27017/?replicaSet=rs0";

    private static GenericContainer<?> mongo;
    private static String saidaDaAplicacao;
    private static String saidaDoDba;

    @BeforeAll
    static void subir() throws Exception {
        mongo = new GenericContainer<>(ENTERPRISE)
                .withCopyToContainer(MountableFile.forHostPath("mongo/queryable-encryption-aplicacao.js"), "/scripts/aplicacao.js")
                .withCopyToContainer(MountableFile.forHostPath("mongo/queryable-encryption-dba.js"), "/scripts/dba.js")
                .withCommand("--replSet", "rs0", "--bind_ip_all")
                .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
        mongo.start();

        // Um replica set de um membro só, e o mongocryptd, que o mongosh usa para cifrar os campos.
        mongosh("--eval", "rs.initiate({ _id: 'rs0', members: [{ _id: 0, host: 'localhost:27017' }] }).ok");
        esperarOPrimario();
        ExecResult mongocryptd = mongo.execInContainer("mongocryptd", "--fork", "--logpath", "/tmp/mongocryptd.log",
                "--pidfilepath", "/tmp/mongocryptd.pid", "--idleShutdownTimeoutSecs", "600");
        assertEquals(0, mongocryptd.getExitCode(), mongocryptd.getStdout());

        saidaDaAplicacao = mongosh(REPLICA_SET, "/scripts/aplicacao.js");
        saidaDoDba = mongosh(REPLICA_SET, "/scripts/dba.js");
        System.out.println("Aplicação, com a chave:\n" + saidaDaAplicacao);
        System.out.println("DBA, sem a chave:\n" + saidaDoDba);
    }

    @AfterAll
    static void descer() {
        mongo.stop();
    }

    @Test
    @DisplayName("a aplicação, com a chave, consulta pelo CPF e recebe o CPF aberto")
    void aAplicacaoLeOCpf() {
        assertTrue(saidaDaAplicacao.contains("nome: 'Maria Souza'"), saidaDaAplicacao);
        assertTrue(saidaDaAplicacao.contains("cpf: '123.456.789-09'"), saidaDaAplicacao);
    }

    @Test
    @DisplayName("o DBA, sem a chave, vê o CPF como Binary.createFromBase64('D…', 6)")
    void oDbaVeOCpfCifrado() {
        assertTrue(saidaDoDba.contains("nome: 'Maria Souza'"), saidaDoDba);
        assertTrue(saidaDoDba.matches("(?s).*cpf: Binary\\.createFromBase64\\('D[^']+', 6\\).*"), saidaDoDba);
        assertTrue(!saidaDoDba.contains("123.456.789-09"), "o CPF aberto não deveria aparecer para o DBA");
    }

    @Test
    @DisplayName("o Queryable Encryption acrescenta o campo __safeContent__ ao documento")
    void safeContent() {
        assertTrue(saidaDoDba.contains("__safeContent__"), saidaDoDba);
    }

    @Test
    @DisplayName("sem a chave, a busca pelo CPF em claro não encontra nada")
    void semAChaveABuscaNaoEncontra() {
        assertTrue(saidaDoDba.contains("busca pelo CPF em claro, sem a chave: 0"), saidaDoDba);
    }

    private static void esperarOPrimario() throws Exception {
        Instant limite = Instant.now().plus(Duration.ofSeconds(30));
        while (!"true".equals(mongosh("--eval", "db.hello().isWritablePrimary"))) {
            if (Instant.now().isAfter(limite)) {
                throw new IllegalStateException("o replica set não elegeu um primário");
            }
            Thread.sleep(500);
        }
    }

    private static String mongosh(String... argumentos) throws Exception {
        String[] comando = new String[argumentos.length + 2];
        comando[0] = "mongosh";
        comando[1] = "--quiet";
        System.arraycopy(argumentos, 0, comando, 2, argumentos.length);
        ExecResult resultado = mongo.execInContainer(comando);
        assertEquals(0, resultado.getExitCode(), resultado.getStdout() + resultado.getStderr());
        return resultado.getStdout().trim();
    }
}
