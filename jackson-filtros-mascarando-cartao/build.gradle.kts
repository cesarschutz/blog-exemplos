plugins {
    java
}

group = "br.com.cesarschutz.exemplos"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 4.1.1 gerencia as versões; o BOM do Jackson sobe o Jackson para a 3.2.3,
    // a versão com que o código do post foi executado (o Gradle fica com a maior das duas).
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation(platform("tools.jackson:jackson-bom:3.2.3"))

    implementation("org.springframework.boot:spring-boot-starter-jackson")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
