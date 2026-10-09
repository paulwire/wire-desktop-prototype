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
| Client + MLS registration (`sessionScope.client.getOrRegister(RegisterClientParam(...))` — one call covers both; confirmed end to end against staging during the issue #11 spike) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/client/RegisterClientUseCase.kt` |
| Conversations (`conversations: ConversationScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/conversation/` (`GetConversationsUseCase.kt`, `ObserveConversationListDetailsUseCaseImpl.kt`) |
| Messages (`messages: MessageScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/message/` (`GetRecentMessagesUseCase.kt`, `SendTextMessageUseCase.kt`) |
| Self user | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/user/` (`GetSelfUserUseCase.kt`, `ObserveSelfUserUseCase.kt`) |
| Proteus/MLS crypto (prebuilt, no native build) | `cryptography/build.gradle.kts` (`com.wire:core-crypto-jvm`, pinned in `gradle/libs.versions.toml`) |

## Dependency mechanism: proven end to end locally; not yet wired into CI (issues #11/#18 spike findings)

Kalium is not published as a consumable Maven artifact anywhere (no `maven-publish`/`publishToMavenLocal` setup exists in `../kalium` as of `develop@1668716594a300fc3ed6b8b202bdf2b0b4689e1c`). The mechanism that resolves and builds against it is a Gradle composite build:

```kotlin
// settings.gradle.kts
includeBuild("../kalium") {
    dependencySubstitution {
        substitute(module("com.wire.kalium:logic")).using(project(":logic"))
    }
}
```

This gets dependency *resolution* working (`com.wire.kalium:logic` substitutes to `:logic`'s project build, `com.wire:core-crypto-jvm:9.1.1` resolves as a normal artifact) but requires further local setup just to build against:

- Our Gradle wrapper bumped from 8.9 to at least 8.11.1 — kalium's `android/` module's AGP enforces that floor.
- JVM heap raised well above Gradle's defaults (512 MiB heap / 384 MiB metaspace was not enough — the daemon crashed from GC thrashing while Gradle configured kalium's entire multi-module graph, which happens regardless of task scoping; 4 GiB heap / 1 GiB metaspace got past configuration).
- Our Kotlin/Compose toolchain upgraded to match kalium's Kotlin 2.1.0 (issue #18 — see below).
- A maven repo mirrored into our own `repositories {}` block for kalium's patched `mockative` dependency — see [ADR 0003](adr/0003-kalium-bootstrap-spike-outcome.md) for why kalium's own repo declaration for it doesn't carry across the included-build boundary.

**Whether kalium's unrelated `:monkeys` module gets pulled into the build is not reliably explained by bare vs. project-scoped task invocation.** One run of bare `./gradlew compileKotlin` triggered a real compile of `:kalium:monkeys` (broken on the snapshot tested against, causing failure); a later run of scoped `./gradlew :compileKotlin` didn't touch it; a subsequent bare `./gradlew compileKotlin --dry-run` *also* didn't touch it, despite being "bare" like the first run. That inconsistency across nominally-identical invocations means the real cause is still unknown — plausibly Gradle task/build-cache state carried over between runs rather than anything about how the task was addressed. **Don't rely on task-scoping alone to avoid this**; treat kalium's monorepo-wide configuration cost and this unpredictability as an open risk of the composite-build approach.

**The Kotlin version mismatch that previously blocked compilation is resolved.** Kalium is built with Kotlin 2.1.0; this project was pinned to Kotlin 1.9.24/Compose 1.6.11 (predating Kotlin 2.x's separate Compose-compiler-plugin model). Issue #18 upgraded both (Kotlin 2.1.0, Compose 1.8.2 + `org.jetbrains.kotlin.plugin.compose`), clearing the "Module was compiled with an incompatible version of Kotlin" failure.

**With that clear, issue #11's spike confirmed the mechanism works end to end, not just for compilation**: a throwaway harness (not committed — see [ADR 0003](adr/0003-kalium-bootstrap-spike-outcome.md)) logged into staging with a real test account, registered a device client, completed MLS key-package upload, and reached a usable `UserSessionScope`, all built against kalium's `:logic` via this composite setup. The exact API call sequence is recorded in ADR 0003 and in the entry-points table below.

**New blocker: CI has no access to kalium.** `.github/workflows/ci.yml` only checks out this repository — there's no sibling `../kalium` checkout, so permanently committing the composite-build wiring above would break CI on every PR. This needs solving before issue #12 (the login screen) can depend on kalium in committed code; it's tracked as a separate issue rather than folded into #11's spike.

**Conclusion**: the composite-build dependency mechanism is proven viable for local development, through login and MLS registration — but isn't committed to this repo yet, pending a fix for the CI-availability gap above. See [ADR 0002](adr/0002-kalium-dependency-mechanism.md) for the original decision record and [ADR 0003](adr/0003-kalium-bootstrap-spike-outcome.md) for this spike's outcome.

See [docs/roadmap.md](roadmap.md) for the integration slices and their open risks.
