@file:Suppress("VulnerableLibrariesLocal")

plugins {
    val kotlinVersion = "2.1.0"
    val ktorVersion = "3.0.1"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.ktor.plugin") version ktorVersion
}

group = "me.kuku"
version = "1.0-SNAPSHOT"

repositories {
//    mavenLocal()
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

fun DependencyHandlerScope.ktor() {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-netty")

    implementation("io.ktor:ktor-serialization-jackson")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-logging")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("com.github.pengrad:java-telegram-bot-api:7.11.0")
    implementation("org.jsoup:jsoup:1.17.2")
    val ociVersion = "3.48.0"
    implementation("com.oracle.oci.sdk:oci-java-sdk-core:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3:$ociVersion") {
        exclude("commons-logging", "commons-logging")
    }
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.google.zxing:javase:3.5.3")
    implementation("com.aallam.openai:openai-client:3.8.2")
    ktor()
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
    }
}

tasks.compileJava {
    options.encoding = "utf-8"
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("tgbot.jar")
}

application {
    mainClass.set("me.kuku.telegram.TelegramApplicationKt")
}
