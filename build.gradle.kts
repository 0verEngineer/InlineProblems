import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("io.freefair.lombok") version "8.6"
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.qodana") version "2024.1.5"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()

    // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))

        // Plugin Dependencies. Uses the `platformPlugins` property from gradle.properties.
        plugins(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })

        pluginVerifier()
        zipSigner()

        // Drives a real IDE for the UI tests, see the testIdeUi task below
        testFramework(TestFrameworkType.Starter)
    }

    testImplementation(platform("org.junit:junit-bom:${properties("junitVersion").get()}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure the IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.provider {
            file("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").let(::markdownToHTML)
        }

        // Get the latest available change notes from the changelog file
        changeNotes = providers.provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion").get())
                        ?: runCatching { getLatest() }.getOrElse { getUnreleased() },
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            // An empty pluginUntilBuild means no upper bound, so the property has to stay unset
            untilBuild = provider { properties("pluginUntilBuild").orNull?.takeIf { it.isNotBlank() } }
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")

        // The pluginVersion is based on SemVer (https://semver.org) and supports pre-release labels,
        // like 2.1.7-alpha.3. If no '-' is found in the pluginVersion the plugin gets published in
        // the default and the beta channel, so users with the beta channel active also see the
        // stable release.
        channels = properties("pluginVersion").map {
            if (it.contains("beta")) listOf("beta") else listOf("default", "beta")
        }
    }

    pluginVerification {
        /* Only genuine breakage fails the build. The plugin knowingly builds on internal API
         * (HighlightInfo, DocumentMarkupModel, FontInfo, ShowIntentionActionsHandler), so
         * INTERNAL_API_USAGES and the deprecation levels would be permanently red without saying
         * anything actionable. They are still listed in the verification report. */
        failureLevel = listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INVALID_PLUGIN,
            FailureLevel.MISSING_DEPENDENCIES,
        )

        ides {
            /* Pinned explicitly. Resolving the IDEs from pluginSinceBuild with an open untilBuild
             * yields more than a dozen builds at several GB each, which is more than a CI runner
             * has. `./gradlew printProductsReleases` prints the available builds. */
            properties("pluginVerifierIdeVersions").get()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { notation ->
                    // "IC-2025.3" -> type IC, version 2025.3. Without a prefix the platformType is used.
                    val parts = notation.split('-', limit = 2)
                    if (parts.size == 2) {
                        create(parts[0], parts[1])
                    } else {
                        create(properties("platformType").get(), parts[0])
                    }
                }
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(file(".qodana").canonicalPath)
    resultsPath.set(file("build/reports/inspections").canonicalPath)
}

/* UI tests run against a real IDE, so they get their own task instead of hanging off `test`.
 * Replaces the runIdeForUiTests task of the old gradle-intellij-plugin, which drove the IDE
 * through the robot-server plugin - the 2.x plugin uses the IntelliJ Starter framework.
 *
 * There are no UI tests yet, so the task currently runs an empty suite. The plumbing is here so
 * that adding the first test under src/test/java is all it takes. */
intellijPlatformTesting.testIdeUi.register("testIdeUi") {
    task {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    test {
        useJUnitPlatform()
    }

    // Pointless without test sources, and running both instrumentation tasks in one build
    // crashes InstrumentCodeTask ("1 >= 1"). instrumentCode still covers the shipped code.
    named("instrumentTestCode") {
        enabled = false
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = properties("javaVersion").get()
        targetCompatibility = properties("javaVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    runIde {
        maxHeapSize = "4g"
        minHeapSize = "2g"
    }
}
