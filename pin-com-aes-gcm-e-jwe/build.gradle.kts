plugins {
    java
}

group = "br.com.cesarschutz.exemplos"

repositories {
    mavenCentral()
}

dependencies {
    // O código principal usa só o JDK (javax.crypto): nada de biblioteca de criptografia de terceiros,
    // para o exemplo mostrar cada decisão (o IV, a AAD, o kid, o algoritmo) sem nada escondido.
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
