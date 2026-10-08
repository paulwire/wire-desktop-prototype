# Architecture

## Layers

Per [AGENTS.md](../AGENTS.md): `ui` → `presentation` → `data`, one-way. `ui` is Compose-only, `presentation` holds ViewModels, `data` is the only layer allowed to import Kalium. Nothing outside `data` sees a Kalium type.

## How Kalium is wired in

- `data` owns one process-scoped `CoreLogic` instance (Kalium's top-level entry point), constructed with a `rootPath` under this app's local data directory.
- Backend endpoints are resolved through Kalium's own custom-backend deep-link mechanism (`fetchServerConfigFromDeepLink(url)`) rather than hand-derived — for staging, the `config=` URL is `https://staging-nginz-https.zinfra.io/deeplink.json`.
- `data` wraps Kalium's scopes and use cases behind its own repository interfaces (session/auth, conversations, messages, self-user). Only `presentation` depends on those interfaces; `ui` never does — it only consumes state/actions exposed by `presentation`'s ViewModels, per AGENTS.md's `ui` → `presentation` → `data` rule.
- Crypto (Proteus and MLS) runs through the prebuilt `com.wire:core-crypto-jvm` Maven artifact — no native library build step required for this JVM-only app.

## Where the session lives

`CoreLogic` → `GlobalKaliumScope` (pre-login: persisted account list) → `AuthenticationScope` (one per login attempt) → `UserSessionScope` (one per logged-in user). `data` holds the active `UserSessionScope` for the single logged-in user (multi-account is out of scope for now).

On app start, `data` checks `GlobalKaliumScope` for a persisted account before showing any screen. A persisted account alone isn't enough — `getSessionScope(userId)` doesn't itself validate that the session is still authenticated — so `data` also confirms the session is actually valid before restoring it with no fresh login. If no account exists, restore fails, or the account turns out to be invalid, the user sees the login screen. See [docs/roadmap.md](roadmap.md) slice 3 for the open question of exactly which Kalium API confirms validity.

## Key Kalium entry points (paths in `../kalium`)

| Entry point | Path |
| --- | --- |
| `CoreLogic` | `logic/src/jvmMain/kotlin/com/wire/kalium/logic/CoreLogic.kt` |
| `GlobalKaliumScope` (via `CoreLogic.getGlobalScope()`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/GlobalKaliumScope.kt` |
| `AuthenticationScope` (via `coreLogic.versionedAuthenticationScope(serverLinks).invoke(null)`, not a direct `getAuthenticationScope(...)` call — confirmed against the `cli` module during the issue #11 spike) | `cli/src/commonMain/kotlin/com/wire/kalium/cli/commands/LoginCommand.kt` |
| `UserSessionScope` (via `CoreLogic.getSessionScope(userId)`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/UserSessionScope.kt` |
| Conversations (`conversations: ConversationScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/conversation/` (`GetConversationsUseCase.kt`, `ObserveConversationListDetailsUseCaseImpl.kt`) |
| Messages (`messages: MessageScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/message/` (`GetRecentMessagesUseCase.kt`, `SendTextMessageUseCase.kt`) |
| Self user | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/user/` (`GetSelfUserUseCase.kt`, `ObserveSelfUserUseCase.kt`) |
| Proteus/MLS crypto (prebuilt, no native build) | `cryptography/build.gradle.kts` (`com.wire:core-crypto-jvm`, pinned in `gradle/libs.versions.toml`) |

## Dependency mechanism: viable, blocked on a Kotlin/Compose upgrade (issue #11 spike finding)

Kalium is not published as a consumable Maven artifact anywhere (no `maven-publish`/`publishToMavenLocal` setup exists in `../kalium` as of `develop@1668716594a300fc3ed6b8b202bdf2b0b4689e1c`). The mechanism that resolves and builds against it is a Gradle composite build:

```kotlin
// settings.gradle.kts
includeBuild("../kalium") {
    dependencySubstitution {
        substitute(module("com.wire.kalium:logic")).using(project(":logic"))
    }
}
```

This gets dependency *resolution* working (`com.wire.kalium:logic` substitutes to `:logic`'s project build, `com.wire:core-crypto-jvm:9.1.1` resolves as a normal artifact) but requires two further fixes just to configure:

- Our Gradle wrapper bumped from 8.9 to at least 8.11.1 — kalium's `android/` module's AGP enforces that floor.
- JVM heap raised well above Gradle's defaults (512 MiB heap / 384 MiB metaspace was not enough — the daemon crashed from GC thrashing while Gradle configured kalium's entire multi-module graph, which happens regardless of task scoping; 4 GiB heap / 1 GiB metaspace got past configuration).

**Prefer project-scoped task invocation (`:compileKotlin`, `:build`, etc.) over bare task names.** Running `./gradlew compileKotlin` (bare) triggered a compile of kalium's unrelated `:monkeys` module (an internal load-testing tool, broken on the snapshot tested against); running `./gradlew :compileKotlin` (root-project-scoped) did not. That's a real, reproducible difference we observed directly — but a PR review correctly challenged the mechanism we first wrote down to explain it (claiming bare task names search included builds), which doesn't match Gradle's own documented task-selection semantics for a single-root-project build like ours. **The exact cause is still unconfirmed** — don't treat the explanation as settled, only the observation (scope task invocations, verify compilation stays clean of unrelated modules before relying on it).

**The real remaining blocker is a Kotlin version mismatch.** Kalium is built with Kotlin 2.1.0; this project is pinned to Kotlin 1.9.24 (via the Compose Multiplatform Gradle plugin version, 1.6.11, which predates Kotlin 2.x's separate Compose-compiler-plugin model). Kalium's compiled classes carry 2.1.0 binary metadata, which a 1.9.x Kotlin compiler can't read — `compileKotlin` fails with "Module was compiled with an incompatible version of Kotlin" across the board, including in our own pre-existing source files, once kalium's Kotlin 2.1.0 stdlib lands on the classpath.

**Conclusion**: the composite-build dependency mechanism is viable, provided (a) all Gradle invocations are project-scoped and (b) this project's Kotlin and Compose Multiplatform versions are upgraded to be binary-compatible with kalium's Kotlin 2.1.0. That upgrade is a separate, non-trivial, whole-codebase change (new Compose-compiler-plugin model, possible detekt/Konsist compatibility implications) — tracked as its own issue rather than folded into the spike. See [ADR 0002](adr/0002-kalium-dependency-mechanism.md) for the full decision record.

See [docs/roadmap.md](roadmap.md) for the integration slices and their open risks.
