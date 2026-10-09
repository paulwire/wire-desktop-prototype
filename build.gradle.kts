plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

repositories {
    google()
    mavenCentral()
    // Mirrors kalium's own allprojects repo (../kalium/build.gradle.kts) for its patched mockative
    // dependency. kalium's repo declaration doesn't carry across the composite-build boundary for
    // our own :runtimeClasspath resolution (see ADR 0003), so it needs to be listed here too.
    maven(url = "https://raw.githubusercontent.com/saleniuk/mockative/fix/duplicates-while-merging-dex-archives-mvn/release")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.wire.kalium:logic:0.0.1") // version is irrelevant: substituted by settings.gradle.kts
    // Version matches kalium's own pin (../kalium/gradle/libs.versions.toml) to avoid classpath
    // conflicts on the shared composite-build graph. Apache-2.0, compatible with this project's GPL-3.0.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.lemonappdev:konsist:0.17.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Needed to reference kalium's Client fixture type (its registrationTime/lastActive fields are
    // kotlinx.datetime.Instant?), not used directly by our own code. Version matches kalium's pin.
    testImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.8")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}

tasks.withType<JavaExec>().configureEach {
    // Snap-packaged editors (e.g. VS Code) set GTK_PATH to their own bundled GTK module dir.
    // GTK then loads a snap-bundled libpthread.so.0 that's incompatible with the system glibc,
    // crashing the JVM with a "GLIBC_PRIVATE" symbol lookup error. Strip it so `run` works from
    // any terminal, regardless of what launched it.
    environment.remove("GTK_PATH")
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
                // Thin delegation to a real Kalium CoreLogic (login, client/MLS registration) that
                // can't be unit tested without a live Kalium session - see issue #12/ADR 0003. All
                // sequencing/branching logic lives in DefaultAuthRepository instead, against the
                // KaliumAuthGateway interface, and is covered (see DefaultAuthRepositoryTest).
                classes("com.wire.desktop.data.auth.DefaultKaliumAuthGateway")
                classes("*AuthRepositoryFactoryKt*")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
