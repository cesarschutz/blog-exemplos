package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * "MongoDB fora do Atlas", em repouso: o MongoDB Enterprise com o Encrypted Storage Engine e o
 * Community sem ele. O mesmo número de cartão é gravado nos dois, e o teste procura o número direto
 * nos arquivos do dbPath (os .wt do WiredTiger e o journal).
 */
@DisplayName("MongoDB: criptografia em repouso no Enterprise e no Community")
class MongoEmRepousoTest {

    private static final DockerImageName ENTERPRISE = DockerImageName.parse("mongodb/mongodb-enterprise-server:8.0-ubuntu2204");
    private static final DockerImageName COMMUNITY = DockerImageName.parse("mongo:8.0");
    private static final String CARTAO = "4111111111111111";
    private static final String GRAVAR = "db.getSiblingDB('banco').cartoes.insertOne({ numero: '" + CARTAO + "' });"
            + " db.adminCommand({ fsync: 1 }).ok";

    private static GenericContainer<?> enterprise;
    private static GenericContainer<?> community;

    @BeforeAll
    static void subir() throws Exception {
        byte[] chave = new byte[32];
        new SecureRandom().nextBytes(chave);
        enterprise = new GenericContainer<>(ENTERPRISE)
                // O Testcontainers copia o arquivo de chave com dono root e permissão 600; o mongod roda
                // como root para conseguir lê-lo (só no teste).
                .withCreateContainerCmdModifier(comando -> comando.withUser("root"))
                .withCopyToContainer(MountableFile.forHostPath("mongo/mongod-arquivo-de-chave.conf"), "/etc/mongod.conf")
                .withCopyToContainer(Transferable.of(Base64.getEncoder().encodeToString(chave), 0600), "/etc/mongodb/chave/mongodb-keyfile")
                .withCommand("--config", "/etc/mongod.conf")
                .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
        community = new GenericContainer<>(COMMUNITY)
                .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
        Startables.deepStart(enterprise, community).join();

        assertEquals("1", mongosh(enterprise, GRAVAR));
        assertEquals("1", mongosh(community, GRAVAR));
    }

    @AfterAll
    static void descer() {
        enterprise.stop();
        community.stop();
    }

    @Test
    @DisplayName("no Enterprise, o Encrypted Storage Engine está ligado")
    void enterpriseComCriptografia() throws Exception {
        assertEquals("true", mongosh(enterprise, "db.serverStatus().encryptionAtRest.encryptionEnabled"));
    }

    @Test
    @DisplayName("no Enterprise, o número não aparece em nenhum arquivo do dbPath")
    void enterpriseNaoMostraONumeroNoDisco() throws Exception {
        assertEquals("", arquivosComONumero(enterprise));
    }

    @Test
    @DisplayName("no Community, o número aparece em claro nos arquivos do WiredTiger e no journal")
    void communityMostraONumeroNoDisco() throws Exception {
        String arquivos = arquivosComONumero(community);
        assertTrue(arquivos.contains("/data/db/collection-") && arquivos.contains("/data/db/journal/"), arquivos);
    }

    @Test
    @DisplayName("o Community não tem a opção: --enableEncryption é recusada")
    void communityNaoTemAOpcao() throws Exception {
        ExecResult resultado = community.execInContainer("mongod", "--enableEncryption", "--dbpath", "/tmp", "--port", "27999");
        assertNotEquals(0, resultado.getExitCode());
        assertTrue((resultado.getStdout() + resultado.getStderr()).contains("unrecognised option '--enableEncryption'"),
                resultado.getStdout() + resultado.getStderr());
    }

    @Test
    @DisplayName("quem se conecta ao banco continua lendo o número: a decifragem acontece por baixo")
    void quemTemAcessoLe() throws Exception {
        assertEquals(CARTAO, mongosh(enterprise, "db.getSiblingDB('banco').cartoes.findOne().numero"));
    }

    /** Os arquivos do banco com o número em claro (fora o log do próprio mongosh, que fica em .mongodb). */
    private static String arquivosComONumero(GenericContainer<?> container) throws Exception {
        return container.execInContainer("grep", "-rl", "--exclude-dir=.mongodb", CARTAO, "/data/db").getStdout().trim();
    }

    private static String mongosh(GenericContainer<?> container, String comando) throws Exception {
        ExecResult resultado = container.execInContainer("mongosh", "--quiet", "--eval", comando);
        assertEquals(0, resultado.getExitCode(), resultado.getStderr());
        return resultado.getStdout().trim();
    }
}
