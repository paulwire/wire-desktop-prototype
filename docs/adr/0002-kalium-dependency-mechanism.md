# 2. Kalium dependency mechanism

## Status

Accepted (the rejection of composite build); the replacement mechanism is not yet decided — tracked in a follow-up issue.

## Context

Issue #11 (the roadmap's spike slice) requires constructing Kalium's `CoreLogic` and logging in from *this* project's own Gradle build, not kalium's own `cli` module. Kalium is not published as a consumable Maven artifact anywhere — no `maven-publish`/`publishToMavenLocal` setup exists in `../kalium` as of `develop@1668716594a300fc3ed6b8b202bdf2b0b4689e1c`.

The only mechanism that resolves the dependency at all is a Gradle composite build (`includeBuild("../kalium")` with an explicit `dependencySubstitution` from `com.wire.kalium:logic` to `project(":logic")`). Getting this far already required bumping our Gradle wrapper (8.9 → 8.11.1, to clear a floor enforced by kalium's `android/` module's AGP version) and raising JVM heap well past Gradle's defaults (to 4 GiB, to stop the daemon crashing from GC thrashing while configuring kalium's entire multi-module graph — composite builds always configure every subproject of an included build, not just the one being depended on).

Even after those fixes, compilation fails: our `compileKotlin` pulls in `:kalium:monkeys:compileKotlin` — an unrelated internal load-testing tool, nothing in our dependency chain — which is broken on the snapshot tested against. No supported way to exclude unrelated included-build subprojects from the task graph was found.

## Decision

Reject a plain Gradle composite build as the dependency mechanism. It makes this project's buildability hostage to the health of kalium's entire monorepo — including modules we have no stake in, can't fix ourselves (kalium is read-only reference material per [CLAUDE.md](../../CLAUDE.md)), and that can break independently of anything we do.

We have not yet decided the replacement. Candidates surfaced but not evaluated in depth: getting kalium to publish `:logic` as a real artifact (requires buy-in we don't control), vendoring a pre-built jar from kalium's own CI output, or some other dependency-substitution scoping we haven't found yet.

## Consequences

Slice 1 (and everything after it in [docs/roadmap.md](../roadmap.md)) is blocked until a real mechanism is found — tracked as a separate issue. This ADR's "accepted" status covers only the rejection of plain composite build; it should be superseded (not edited) once a replacement mechanism is chosen.
