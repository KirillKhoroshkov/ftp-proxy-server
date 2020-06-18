plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "org.polykek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.0.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.13.3")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.3")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}