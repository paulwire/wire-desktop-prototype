pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wire-desktop-prototype"

// Kalium isn't published as a consumable Maven artifact (see ADR 0002), so this composite build
// is the dependency mechanism: it resolves `com.wire.kalium:logic` from a sibling checkout's
// `:logic` project instead. CI checks out that sibling itself (.github/workflows/ci.yml); locally
// it must be a checkout of https://github.com/wireapp/kalium at ../kalium.
includeBuild("../kalium") {
    dependencySubstitution {
        substitute(module("com.wire.kalium:logic")).using(project(":logic"))
    }
}
