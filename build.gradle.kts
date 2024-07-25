@file:Suppress("VulnerableLibrariesLocal")

plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization").version(kotlinVersion)
}

group = "me.kuku"
version = "1.0-SNAPSHOT"

val ktorVersion = "2.3.12"

repositories {
//    mavenLocal()
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-thymeleaf:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("com.github.pengrad:java-telegram-bot-api:7.7.0")
    implementation("me.kuku:utils:2.3.12.1")
    implementation("org.jsoup:jsoup:1.17.2")
    val ociVersion = "3.44.1"
    implementation("com.oracle.oci.sdk:oci-java-sdk-core:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:$ociVersion")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3:$ociVersion") {
        exclude("commons-logging", "commons-logging")
    }
    implementation("com.google.zxing:javase:3.5.3")
    implementation("net.consensys.cava:cava-bytes:0.5.0")
    implementation("net.consensys.cava:cava-crypto:0.5.0")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.1.2")
    implementation("org.mongodb:bson-kotlinx:5.1.2")

    testImplementation(kotlin("test"))
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