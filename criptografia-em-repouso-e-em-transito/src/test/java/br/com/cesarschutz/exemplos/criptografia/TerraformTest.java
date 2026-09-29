package br.com.cesarschutz.exemplos.criptografia;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * O Terraform do artigo: o do RDS é validado e aplicado contra o moto (AWS simulada); o do Atlas só
 * é validado, porque aplicar exige uma conta no Atlas. Os provedores vêm do registry do Terraform.
 */
@DisplayName("Terraform: o RDS aplicado no moto e o Atlas validado")
class TerraformTest {

    private static final DockerImageName TERRAFORM = DockerImageName.parse("hashicorp/terraform:1.16.4");
    private static final DockerImageName MOTO = DockerImageName.parse("motoserver/moto:5.2.2");

    private static final Network REDE = Network.newNetwork();
    private static GenericContainer<?> moto;

    @BeforeAll
    static void subir() {
        moto = new GenericContainer<>(MOTO)
                .withNetwork(REDE)
                .withNetworkAliases("moto")
                .withExposedPorts(5000)
                .waitingFor(Wait.forHttp("/moto-api/").forPort(5000));
        moto.start();
    }

    @AfterAll
    static void descer() {
        moto.stop();
        REDE.close();
    }

    @Test
    @DisplayName("RDS: o aws_db_instance do artigo sobe com storage_encrypted e a chave pelo ARN")
    void rdsNoMoto() {
        String saida = terraform("rds", """
                terraform init -input=false -no-color \
                && terraform apply -auto-approve -input=false -no-color \
                && terraform state show -no-color aws_db_instance.pedidos""");
        assertTrue(saida.contains("Apply complete! Resources: 2 added"), saida);
        assertTrue(saida.matches("(?s).*storage_encrypted\\s+= true.*"), saida);
        assertTrue(saida.matches("(?s).*kms_key_id\\s+= \"arn:aws:kms:sa-east-1:[0-9]+:key/.*"), saida);
    }

    @Test
    @DisplayName("Atlas: o mongodbatlas_encryption_at_rest e o cluster do artigo passam no terraform validate")
    void atlasValida() {
        String saida = terraform("atlas", """
                terraform init -input=false -no-color -backend=false \
                && terraform validate -no-color""");
        assertTrue(saida.contains("Success! The configuration is valid."), saida);
    }

    /**
     * Roda os comandos numa cópia da pasta terraform/<pasta> e devolve o que o container escreveu. Na
     * pasta do RDS entra também o provedor apontando para o moto (src/test/resources/terraform).
     */
    private static String terraform(String pasta, String comandos) {
        try (GenericContainer<?> terraform = new GenericContainer<>(TERRAFORM)) {
            terraform.withNetwork(REDE)
                    .withCopyToContainer(MountableFile.forHostPath("terraform/" + pasta), "/trabalho")
                    .withWorkingDirectory("/trabalho")
                    .withCreateContainerCmdModifier(comando -> comando.withEntrypoint("sh", "-c"))
                    // Um argumento só: o withCommand(String) quebraria os comandos nos espaços.
                    .withCommand(new String[] {comandos})
                    .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(10)));
            if ("rds".equals(pasta)) {
                terraform.withCopyToContainer(MountableFile.forClasspathResource("terraform/provedor-moto.tf"), "/trabalho/provedor-moto.tf");
            }
            usarEspelhoLocalSeHouver(terraform);
            try {
                terraform.start();
            } catch (RuntimeException erro) {
                throw new AssertionError("o terraform terminou com erro:\n" + terraform.getLogs(), erro);
            }
            return terraform.getLogs();
        }
    }

    /**
     * Só para rodar sem acesso ao registry do Terraform: com TF_ESPELHO apontando para um diretório no
     * formato de filesystem mirror (com o hashicorp/aws 6.66.0 e o mongodb/mongodbatlas 2.18.0), os
     * provedores vêm dele. No GitHub Actions e numa máquina comum, a variável fica vazia.
     */
    private static void usarEspelhoLocalSeHouver(GenericContainer<?> terraform) {
        String espelho = System.getenv("TF_ESPELHO");
        if (espelho == null || espelho.isBlank()) {
            return;
        }
        terraform.withFileSystemBind(espelho, "/espelho", BindMode.READ_ONLY)
                .withCopyToContainer(Transferable.of("provider_installation {\n  filesystem_mirror {\n    path = \"/espelho\"\n  }\n}\n"),
                        "/espelho.tfrc")
                .withEnv("TF_CLI_CONFIG_FILE", "/espelho.tfrc");
    }
}
