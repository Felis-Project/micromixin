@file:Suppress("VulnerableLibrariesLocal")

plugins {
    alias(libs.plugins.felis.dam)
    `maven-publish`
}

group = "felis"
version = "1.6.0-alpha+mm${libs.versions.micromixin.get()}"

loaderMake {
    version = "1.21"
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(libs.felis)
    compileOnlyApi(libs.mm.annotations)
    runtimeOnly(libs.mm.runtime)
    implementation(libs.mm.transformer)

    include(libs.mm.transformer)
    include("org.json:json:20230618")
    include(libs.mm.runtime)
}

tasks.processResources {
    filesMatching("felis.mod.toml") {
        expand(
            "version" to version,
            "mm_version" to libs.versions.micromixin.get()
        )
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                artifactId = "micromixin"
            }
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "Repsy"
            url = uri("https://repo.repsy.io/mvn/0xjoemama/public")
            credentials {
                username = System.getenv("REPSY_USERNAME")
                password = System.getenv("REPSY_PASSWORD")
            }
        }
    }
}
