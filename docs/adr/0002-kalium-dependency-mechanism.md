# 2. Kalium dependency mechanism

## Status

Accepted

## Context

Issue #11 (the roadmap's spike slice) requires constructing Kalium's `CoreLogic` and logging in from *this* project's own Gradle build, not kalium's own `cli` module. Kalium is not published as a consumable Maven artifact anywhere — no `maven-publish`/`publishToMavenLocal` setup exists in `../kalium` as of `develop@1668716594a300fc3ed6b8b202bdf2b0b4689e1c`.

The only mechanism that resolves the dependency at all is a Gradle composite build (`includeBuild("../kalium")` with an explicit `dependencySubstitution` from `com.wire.kalium:logic` to `project(":logic")`). Getting this far required bumping our Gradle wrapper (8.9 → 8.11.1, to clear a floor enforced by kalium's `android/` module's AGP version) and raising JVM heap well past Gradle's defaults (to 4 GiB, to stop the daemon crashing from GC thrashing while configuring kalium's entire multi-module graph — this happens regardless of task scoping, since Gradle's configuration phase evaluates every subproject of an included build).

An earlier version of this ADR concluded composite build was unviable, based on `compileKotlin` (invoked as a bare task name) triggering a compile of `:kalium:monkeys` — an unrelated, broken internal load-testing module. A PR review caught that this was an artifact of invocation, not a Gradle limitation: a bare task name runs on every project in the whole build tree, including unrelated included-build subprojects, with a matching task name. Re-running scoped to this project (`./gradlew :compileKotlin`) never touches `:monkeys`.

With that corrected, a real and different blocker surfaced: kalium is built with Kotlin 2.1.0; this project is pinned to Kotlin 1.9.24 (via the Compose Multiplatform Gradle plugin, 1.6.11, which predates Kotlin 2.x's separate Compose-compiler-plugin model). Kalium's compiled classes carry Kotlin 2.1.0 binary metadata that a 1.9.x compiler can't read, so `compileKotlin` fails with "Module was compiled with an incompatible version of Kotlin" — not just for kalium's code, but for our own pre-existing source once kalium's newer Kotlin stdlib lands on the shared classpath.

## Decision

Use a Gradle composite build as the dependency mechanism, with two conditions:

1. All Gradle invocations touching this dependency must be project-scoped (`:compileKotlin`, `:build`, `:run`, etc.), never bare task names — this keeps unrelated kalium subprojects out of the task graph entirely.
2. This project's Kotlin and Compose Multiplatform versions must be upgraded to be binary-compatible with kalium's Kotlin 2.1.0 before any code can actually compile against `:logic`. That upgrade is its own non-trivial, whole-codebase change (new Compose-compiler-plugin model, possible detekt/Konsist compatibility implications) and is tracked as a separate issue rather than folded into the spike.

## Consequences

Issue #11 stays blocked, but on a much narrower and more tractable thing than originally concluded: a Kotlin/Compose Multiplatform version upgrade, not an open-ended "find a different dependency mechanism" search. Once that upgrade lands, the composite-build wiring above (plus the Gradle wrapper and heap adjustments) should let the spike resume. Any future Gradle command against this dependency — in docs, scripts, or CI — must use project-scoped task paths; a bare task name will silently misrepresent what's actually required to build.
