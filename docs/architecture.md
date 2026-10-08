# Architecture

## Layers

Per [AGENTS.md](../AGENTS.md): `ui` → `presentation` → `data`, one-way. `ui` is Compose-only, `presentation` holds ViewModels, `data` is the only layer allowed to import Kalium. Nothing outside `data` sees a Kalium type.

## How Kalium is wired in

- `data` owns one process-scoped `CoreLogic` instance (Kalium's top-level entry point), constructed with a `rootPath` under this app's local data directory.
- Backend endpoints are resolved through Kalium's own custom-backend deep-link mechanism (`fetchServerConfigFromDeepLink(url)`) rather than hand-derived — for staging, the `config=` URL is `https://staging-nginz-https.zinfra.io/deeplink.json`.
- `data` wraps Kalium's scopes and use cases behind its own repository interfaces (session/auth, conversations, messages, self-user). `presentation` and `ui` only ever see those interfaces.
- Crypto (Proteus and MLS) runs through the prebuilt `com.wire:core-crypto-jvm` Maven artifact — no native library build step required for this JVM-only app.

## Where the session lives

`CoreLogic` → `GlobalKaliumScope` (pre-login: persisted account list) → `AuthenticationScope` (one per login attempt) → `UserSessionScope` (one per logged-in user). `data` holds the active `UserSessionScope` for the single logged-in user (multi-account is out of scope for now).

On app start, `data` checks `GlobalKaliumScope` for a persisted account before showing any screen: if one exists, it restores that `UserSessionScope` from the same `rootPath` with no fresh login; otherwise the user sees the login screen.

## Key Kalium entry points (paths in `../kalium`)

| Entry point | Path |
| --- | --- |
| `CoreLogic` | `logic/src/jvmMain/kotlin/com/wire/kalium/logic/CoreLogic.kt` |
| `GlobalKaliumScope` (via `CoreLogic.getGlobalScope()`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/GlobalKaliumScope.kt` |
| `AuthenticationScope` (via `CoreLogic.getAuthenticationScope(...)`); login example | `cli/src/commonMain/kotlin/.../commands/LoginCommand.kt` |
| `UserSessionScope` (via `CoreLogic.getSessionScope(userId)`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/UserSessionScope.kt` |
| Conversations (`conversations: ConversationScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/conversation/` (`GetConversationsUseCase.kt`, `ObserveConversationListDetailsUseCaseImpl.kt`) |
| Messages (`messages: MessageScope`) | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/message/` (`GetRecentMessagesUseCase.kt`, `SendTextMessageUseCase.kt`) |
| Self user | `logic/src/commonMain/kotlin/com/wire/kalium/logic/feature/user/` (`GetSelfUserUseCase.kt`, `ObserveSelfUserUseCase.kt`) |
| Proteus/MLS crypto (prebuilt, no native build) | `cryptography/build.gradle.kts` (`com.wire:core-crypto-jvm`, pinned in `gradle/libs.versions.toml`) |

See [docs/roadmap.md](roadmap.md) for the integration slices and their open risks (MLS client/key-package registration in particular is still being de-risked in the spike).
