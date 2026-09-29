package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * "PostgreSQL no Amazon RDS", em repouso: os comandos do artigo na AWS CLI de verdade, contra o moto,
 * um simulador open source da AWS. O moto confere a API (os parâmetros, o alias da chave, o que cada
 * recurso devolve), mas não cifra nada: a criptografia do storage só acontece na AWS.
 */
@DisplayName("AWS: os comandos do artigo na AWS CLI, contra o moto")
class AwsCliTest {

    private static final DockerImageName MOTO = DockerImageName.parse("motoserver/moto:5.2.2");
    private static final DockerImageName AWS_CLI = DockerImageName.parse("amazon/aws-cli:2.37.5");

    private static final Network REDE = Network.newNetwork();
    private static GenericContainer<?> moto;
    private static GenericContainer<?> cli;

    @BeforeAll
    static void subir() {
        moto = new GenericContainer<>(MOTO)
                .withNetwork(REDE)
                .withNetworkAliases("moto")
                .withExposedPorts(5000)
                .waitingFor(Wait.forHttp("/moto-api/").forPort(5000));
        cli = new GenericContainer<>(AWS_CLI)
                .withNetwork(REDE)
                // A CLI manda tudo para o moto, com credenciais falsas; os scripts ficam como no artigo.
                .withEnv("AWS_ENDPOINT_URL", "http://moto:5000")
                .withEnv("AWS_ACCESS_KEY_ID", "teste")
                .withEnv("AWS_SECRET_ACCESS_KEY", "teste")
                .withEnv("AWS_DEFAULT_REGION", "sa-east-1")
                .withCopyToContainer(MountableFile.forHostPath("aws"), "/aws")
                .withCreateContainerCmdModifier(comando -> comando.withEntrypoint("sleep"))
                .withCommand("infinity");
        Startables.deepStart(moto, cli).join();
    }

    @AfterAll
    static void descer() {
        cli.stop();
        moto.stop();
        REDE.close();
    }

    /** Cada teste começa com uma AWS vazia, e com a chave KMS e o alias alias/rds-pedidos criados. */
    @BeforeEach
    void zerarAAws() throws Exception {
        HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://" + moto.getHost() + ":" + moto.getMappedPort(5000) + "/moto-api/reset"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        executar("bash", "/aws/preparar-chave.sh");
    }

    @Test
    @DisplayName("create-db-instance com --storage-encrypted e a chave pelo alias: a instância nasce criptografada")
    void criarInstanciaCriptografada() throws Exception {
        String saida = executar("bash", "/aws/criar-instancia.sh");
        System.out.println(saida);

        assertEquals("True", aws("rds", "describe-db-instances", "--db-instance-identifier", "pedidos-db",
                "--query", "DBInstances[0].StorageEncrypted", "--output", "text"));
        // A tabela do describe-db-instances do artigo mostra a instância, criptografada e com a chave.
        assertTrue(saida.contains("pedidos-db") && saida.contains("True"), saida);
    }

    @Test
    @DisplayName("instância que já existe sem criptografia: snapshot, cópia com a chave KMS e restauração")
    void migrarInstanciaSemCriptografia() throws Exception {
        // A instância antiga, criada sem --storage-encrypted (não está no artigo: é o ponto de partida).
        aws("rds", "create-db-instance",
                "--db-instance-identifier", "pedidos-db",
                "--engine", "postgres",
                "--db-instance-class", "db.t4g.medium",
                "--allocated-storage", "50",
                "--master-username", "pgadmin",
                "--manage-master-user-password");
        assertEquals("False", aws("rds", "describe-db-instances", "--db-instance-identifier", "pedidos-db",
                "--query", "DBInstances[0].StorageEncrypted", "--output", "text"));

        executar("bash", "/aws/migrar-instancia.sh");

        assertEquals("False", aws("rds", "describe-db-snapshots", "--db-snapshot-identifier", "pedidos-sem-cripto",
                "--query", "DBSnapshots[0].Encrypted", "--output", "text"));
        assertEquals("True", aws("rds", "describe-db-snapshots", "--db-snapshot-identifier", "pedidos-cripto",
                "--query", "DBSnapshots[0].Encrypted", "--output", "text"));
        assertEquals("True", aws("rds", "describe-db-instances", "--db-instance-identifier", "pedidos-db-v2",
                "--query", "DBInstances[0].StorageEncrypted", "--output", "text"));

        // A chave da instância nova é a do alias alias/rds-pedidos.
        String chave = aws("kms", "describe-key", "--key-id", "alias/rds-pedidos", "--query", "KeyMetadata.Arn", "--output", "text");
        assertEquals(chave, aws("rds", "describe-db-instances", "--db-instance-identifier", "pedidos-db-v2",
                "--query", "DBInstances[0].KmsKeyId", "--output", "text"));
    }

    private static String aws(String... argumentos) throws Exception {
        String[] comando = new String[argumentos.length + 1];
        comando[0] = "aws";
        System.arraycopy(argumentos, 0, comando, 1, argumentos.length);
        return executar(comando).trim();
    }

    private static String executar(String... comando) throws Exception {
        ExecResult resultado = cli.execInContainer(comando);
        assertEquals(0, resultado.getExitCode(), String.join(" ", comando) + "\n" + resultado.getStdout() + resultado.getStderr());
        return resultado.getStdout();
    }
}
