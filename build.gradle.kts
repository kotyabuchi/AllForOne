val jdaVersion: String by project
val logbackVersion: String by project
val jsoupVersion: String by project
val sqliteJDBCVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    application

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.kotyabuchi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJDBCVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("com.github.kotyabuchi.AllForOne.Main")
}