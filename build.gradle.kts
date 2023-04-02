plugins {
    val kotlinVersion = "1.8.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    id("org.springframework.boot") version "3.0.5"
    id("io.spring.dependency-management") version "1.1.0"
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
    implementation("org.telegram:telegrambots:6.5.0")
    implementation("org.telegram:telegrambots-abilities:6.5.0")
    implementation("me.kuku:utils:2.2.4.0")
    implementation("me.kuku:ktor-spring-boot-starter:2.2.4.0")
    implementation("org.jsoup:jsoup:1.15.3")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
    }
}

tasks.compileJava {
    options.encoding = "utf-8"
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
