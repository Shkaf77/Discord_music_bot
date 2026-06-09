plugins {
    kotlin("jvm") version "2.3.0"
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
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}