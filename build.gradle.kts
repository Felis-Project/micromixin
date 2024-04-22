plugins {
    alias(libs.plugins.felis.dam)
    `maven-publish`
}

group = "felis"
version = "1.2.0-alpha"

val mmVersion = "0.4.0-a20240227"

loaderMake {
    version = "1.20.4"
}

dependencies {
    implementation(libs.felis)
    compileOnlyApi(libs.mm.annotations)
    runtimeOnly(libs.mm.runtime)
    implementation(libs.mm.transformer)
}

tasks.processResources {
    filesMatching("mods.toml") {
        expand("version" to version)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
