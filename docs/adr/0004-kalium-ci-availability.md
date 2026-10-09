# 4. Kalium CI availability

## Status

Accepted

## Context

[ADR 0002](0002-kalium-dependency-mechanism.md) and [ADR 0003](0003-kalium-bootstrap-spike-outcome.md) established and proved out a Gradle composite-build dependency on kalium's `:logic` module (`includeBuild("../kalium")` + dependency substitution), but only ever applied it locally, then reverted it — `.github/workflows/ci.yml` only checks out this repository, so committing that wiring as-is would break CI on every PR. Issue #12 (the login screen) needs a real, committed dependency on kalium, so this had to be solved first.

## Decision

Commit the composite-build wiring for real, and give CI a sibling kalium checkout to resolve it against:

- `.github/workflows/ci.yml` adds a second `actions/checkout` step for `wireapp/kalium`, with `path: ../kalium` so it lands as a sibling of the main checkout — the same relative layout local development already uses.
- That checkout is **pinned to an exact commit SHA** (`1668716594a300fc3ed6b8b202bdf2b0b4689e1c` — the one ADR 0002/0003's findings were verified against), not a branch like `develop`. Kalium's own build has already shown enough moving parts (Kotlin version floors, a patched dependency, an unpredictable unrelated-module inclusion — see ADR 0002/0003) that tracking a branch would mean CI could break at any time for reasons entirely outside this repo's control. Bumping the pin is a deliberate, reviewable one-line change instead.
- CI's Gradle invocations switch from bare task names (`./gradlew test`) to project-scoped ones (`./gradlew :test`). ADR 0002 recorded that a bare task name pulled kalium's unrelated, broken `:monkeys` module into the build at least once, though the exact trigger was never pinned down. Scoping is a precaution given that uncertainty, not a confirmed fix.
- `settings.gradle.kts`, `build.gradle.kts` (the `com.wire.kalium:logic` dependency, plus mirroring kalium's patched-`mockative` maven repo — see ADR 0003 for why that mirror is necessary), `gradle.properties` (raised JVM heap), and the Gradle wrapper (bumped to 8.11.1) all now carry this permanently, rather than being applied-then-reverted for a single verification run.

## Consequences

Issue #12 (and later slices) can now add real `data`-layer code against kalium's `:logic` module, in CI as well as locally. Bumping kalium's pinned commit is a deliberate action, not automatic — stale pins are a known trade-off of this choice, and whoever picks that up should check ADR 0002/0003's findings still hold at the new commit (Kotlin version, the mockative repo mirror, the `:monkeys`-inclusion risk) before assuming otherwise.
