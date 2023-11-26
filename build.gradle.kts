@file:Suppress("VulnerableLibrariesLocal")

plugins {
    val kotlinVersion = "1.9.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    application
}

group = "me.kuku"
version = "1.0-SNAPSHOT"

repositories {
//    mavenLocal()
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.ehcache:ehcache:3.10.8")
    implementation("com.github.pengrad:java-telegram-bot-api:6.9.1")
    implementation("me.kuku:utils:2.3.5.0")
    implementation("me.kuku:ktor-spring-boot-starter:2.3.5.0")
    implementation("org.jsoup:jsoup:1.16.1")
    val ociVersion = "3.29.0"
    implementation("com.oracle.oci.sdk:oci-java-sdk-core:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3:$ociVersion") {
        exclude("commons-logging", "commons-logging")
    }
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.google.zxing:javase:3.5.2")
    implementation("net.consensys.cava:cava-bytes:0.5.0")
    implementation("net.consensys.cava:cava-crypto:0.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "21"
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

application {
    mainClass = "me.kuku.telegram.TelegramApplicationKt"
}