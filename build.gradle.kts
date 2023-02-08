val jdaVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    application
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
}


kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("com.github.kotyabuchi.AllForOne.Main.kt")
}