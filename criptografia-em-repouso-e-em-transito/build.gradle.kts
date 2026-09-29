plugins {
    java
}

group = "br.com.cesarschutz.exemplos"

repositories {
    mavenCentral()
}

dependencies {
    // Os testes sobem os bancos e a AWS simulada em containers (Docker) e conferem o que o artigo mostra.
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.postgresql:postgresql:42.7.13")
    // Só para gerar as CAs e os certificados de teste em tempo de execução (nada de chave no repositório).
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
