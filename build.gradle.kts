plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.11.0"
    id("io.freefair.lombok") version "6.6"
}

group = "org.OverEngineer"
version = "0.2.1"

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.projectlombok:lombok:1.18.24")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.2.4")
    type.set("IC")

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("212.48")
        untilBuild.set("231.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        maxHeapSize = "4g"
        minHeapSize = "2g"
    }
}
