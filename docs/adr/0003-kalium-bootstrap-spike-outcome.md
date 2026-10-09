# 3. Kalium bootstrap spike outcome

## Status

Accepted

## Context

Issue #11's spike needed to prove `CoreLogic` can be constructed, log in against staging, and complete MLS client/key-package registration — running inside this project's own Gradle/JVM setup. [ADR 0002](0002-kalium-dependency-mechanism.md) had already established the composite-build dependency mechanism was *structurally* viable, but left two things unresolved: whether login/MLS registration actually works end to end once compilation succeeds, and whether the JVM target needs system libsodium installed.

Issue #18 (Kotlin 1.9.24/Compose 1.6.11 → Kotlin 2.1.0/Compose 1.8.2) landed first, clearing the binary-incompatibility blocker ADR 0002 identified. This spike resumed from there.

## Decision

Confirmed, with a throwaway harness (not committed — see Consequences) run against staging with a real test account, that the full sequence works end to end:

1. `CoreLogic(rootPath, KaliumConfigs(), userAgent)` constructs cleanly on the JVM.
2. `coreLogic.globalScope { fetchServerConfigFromDeepLink(url) }` resolves `ServerConfig.Links` for staging.
3. `coreLogic.versionedAuthenticationScope(serverLinks).invoke(null)` yields an `AuthenticationScope`; `authenticationScope.login(email, password, shouldPersistClient = true, ...)` succeeds. On `AuthenticationResult.Failure.InvalidCredentials.Missing2FA`, the harness first calls `authenticationScope.requestSecondFactorVerificationCode(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)` to trigger the email, then prompts for the code and retries login with it — `Invalid2FA` just re-prompts without re-requesting. Matches kalium's own `cli` module's `LoginCommand.kt`.
4. `coreLogic.globalScope { addAuthenticatedAccount(serverConfigId, ssoID, authData, null, true) }` persists the account.
5. `coreLogic.sessionScope(userId) { client.getOrRegister(RegisterClientParam(password, emptyList())) }` registers a device client **and** completes MLS key-package upload in the same call — `RegisterClientUseCase` invokes `RegisterMLSClientUseCase` internally whenever MLS registration is allowed, so there is no separate MLS step to wire up.
6. `coreLogic.getSessionScope(userId)` returns a usable `UserSessionScope`.

This exact call sequence is already the one kalium's own `cli/src/commonMain/kotlin/com/wire/kalium/cli/commands/LoginCommand.kt` and `CLIApplication.kt` use — no undocumented API surface was needed.

**The libsodium-bindings JVM question (left open by the roadmap) is resolved**: the JVM artifact for `com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings` bundles prebuilt native libsodium binaries for linux-x64/arm64, macOS, and Windows x64 directly inside its jar, loaded via JNA at runtime. No system libsodium install is required. The same check on `com.wire:core-crypto-jvm` (the MLS/Rust FFI layer) found it bundles native libs for linux-x86-64 and darwin-aarch64 only — not linux-arm64 or Windows — a latent gotcha if a dev machine is ever something other than those two, but not a blocker for the current staging dev setup (confirmed linux-x64).

Two further composite-build wrinkles surfaced, beyond the wrapper-version and JVM-heap fixes ADR 0002 already recorded:

- `DelicateKaliumApi` lives in kalium's `:util` module, which isn't transitively exposed by depending on `com.wire.kalium:logic` alone — opting into it without also depending on `:util` directly fails to resolve at compile time. Since it's `RequiresOptIn.Level.WARNING`, the simplest fix is to not opt in at all (a build-log warning, not an error) rather than add the extra module dependency.
- kalium patches a dependency (`io.mockative:mockative` → a forked `3.0.1-fix` build) via its own `allprojects { repositories {...} }` block. That per-project repository declaration isn't picked up when resolving configurations in our own root project across the included-build boundary — Gradle's project-repositories-vs-centralized-`dependencyResolutionManagement` resolution model doesn't merge an included build's project repos into the consuming project's graph. Any task that resolves `:runtimeClasspath` (not just `:compileKotlin`) needs that same repo mirrored into our own project's `repositories {}` block.

## Consequences

Issue #11's core question is answered: the composite-build mechanism works end to end for login and MLS registration, not just compilation. The harness used to prove this was deliberately **not committed** — the roadmap scoped it as throwaway exploration code, and its job was to produce this record, not to ship. The composite-build wiring (`includeBuild`, wrapper bump, heap raise, the mockative-repo mirror) was applied locally for this verification and then reverted; it isn't present in this repo's committed `settings.gradle.kts` either.

That's deliberate, because of a new blocker this spike surfaced: **CI has no access to kalium.** `.github/workflows/ci.yml` only checks out this repository — there's no sibling `../kalium` checkout, so permanently committing the composite-build wiring would break CI on every PR. This must be solved before issue #12 (the login screen, which needs a real committed dependency on kalium) can proceed. Tracked as a new, separate issue rather than folded into this one, per the issue workflow's "stop and propose a new issue" guidance.
