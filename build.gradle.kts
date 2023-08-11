import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.ekenstein", "haengma", "2.2.6")
    implementation("org.jetbrains.kotlinx", "kotlinx-cli", "0.3.4")
    implementation("io.github.microutils", "kotlin-logging-jvm", "2.0.11")
    implementation("ch.qos.logback", "logback-classic", "1.2.6")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}