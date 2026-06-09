plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("net.dv8tion:JDA:5.6.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("dev.arbjerg:lavalink-client:3.4.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
    maven("https://maven.lavalink.dev/releases")
}