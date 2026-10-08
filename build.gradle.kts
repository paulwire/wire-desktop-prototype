plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.lemonappdev:konsist:0.17.3")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.8")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}

val test by tasks.existing(Test::class) {
    useJUnitPlatform()
    exclude("**/architecture/**")
}

val konsistTest by tasks.registering(Test::class) {
    description = "Runs Konsist architecture and hygiene tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    include("**/architecture/**")
}

tasks.check {
    dependsOn(konsistTest)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
}

kover {
    reports {
        filters {
            excludes {
                packages("com.wire.desktop.ui")
                classes("*MainKt*")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
