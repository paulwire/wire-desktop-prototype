# Roadmap: real Kalium integration

Vertical slices to wire this app's `data` layer to a real Kalium `logic` session (login, conversations, messaging), replacing the current `GreetingRepository` stub. See [docs/architecture.md](architecture.md) for how Kalium is wired in.

Scope, for every slice below: **no calling**, and **no changes to how the client connects to Wire servers** (backend config is resolved through Kalium's own `fetchServerConfigFromDeepLink`, never hand-derived) — this is additive integration against Kalium's existing surface, not a protocol/backend change. Multi-account is out of scope; single logged-in user at a time.

The target backend for development is staging (deeplink config: `https://staging-nginz-https.zinfra.io/deeplink.json`). The test team defaults to MLS, so MLS is in scope from the first slice — it can't be deferred to "later."

New slices should use the [slice issue template](../.github/ISSUE_TEMPLATE/slice.md).

## 1. Spike: bootstrap Kalium + MLS client registration

**Goal**: Prove `CoreLogic` can be constructed, log in against staging, and complete whatever device/key-package registration MLS requires — running inside this project's own Gradle/JVM setup, not kalium's `cli` module.

**User-visible outcome**: None in the running app yet — this is a technical spike. Success is observed via console/log output and a passing manual run, not through any UI.

**Acceptance criteria**:
- A throwaway `data`-layer entry point (e.g. a small harness, not shipped UI) constructs `CoreLogic` with a `rootPath` under this app's local data directory.
- It resolves `ServerConfig.Links` for staging via `fetchServerConfigFromDeepLink("https://staging-nginz-https.zinfra.io/deeplink.json")`.
- It logs in with the test account's credentials, completes client/key-package registration, and reaches a usable `UserSessionScope` without crashing.
- The open "does the libsodium-bindings JVM target need system libsodium" question is resolved one way or the other, and the answer is written down (promote to an ADR if the resulting approach is non-obvious).

**Kalium APIs involved**: `CoreLogic`, `fetchServerConfigFromDeepLink`, `AuthenticationScope.login`, `CoreLogic.globalScope { addAuthenticatedAccount(...) }`, `CoreLogic.getSessionScope(userId)`, client/key-package registration APIs (exact classes TBD by this spike).

**What to test**: No automated tests for this slice — the harness is throwaway exploration code, not merged into `src/`, so AGENTS.md's per-class unit test rule doesn't apply to it (that rule governs committed `presentation`/`data` classes, not disposable spike code). Manual verification only, written up as findings.

**Out of scope**: Any UI. MLS edge cases beyond "login succeeds on this one team" (external join, key package exhaustion/rotation). Calling.

**Dependencies**: Staging backend reachable; test account credentials; the deeplink config URL above.

## 2. Login screen

**Goal**: Replace the greeting screen with a real login screen that authenticates against staging through `data`.

**User-visible outcome**: The user enters email + password in the app and sees either a logged-in state or a visible error — no more hardcoded greeting text.

**Acceptance criteria**:
- `ui` renders a login form (email, password, submit) that only talks to `presentation`.
- `presentation` exposes login state (idle/loading/success/error) backed by a `data`-layer session/auth interface — no Kalium types cross into `presentation`.
- Submitting valid test-account credentials reaches a logged-in state, reusing whatever client/MLS registration the spike established.
- Submitting invalid credentials shows an error message without crashing.

**Kalium APIs involved**: Whatever `AuthenticationScope`/`CoreLogic` entry points the spike validated, wrapped behind a `data`-layer interface (e.g. a session/auth repository).

**What to test**: Unit tests for the `presentation` ViewModel's state transitions (loading/success/error) against a faked `data` interface; unit test for the `data` login wrapper's success/error mapping. Test names follow `givenX_whenY_thenZ` per AGENTS.md.

**Out of scope**: Session persistence across restarts (slice 3). SSO/team login flows. Remembering a "last used" email.

**Dependencies**: Slice 1 — needs a working login + registration path to wrap.

## 3. Session persistence / restore

**Goal**: Don't require re-login every time the app starts if a session already exists on disk.

**User-visible outcome**: Relaunching the app after a successful login goes straight to the logged-in state, skipping the login screen.

**Acceptance criteria**:
- On app start, `data` checks `GlobalKaliumScope`'s persisted account list before showing any screen.
- If a persisted account exists, `data` restores its `UserSessionScope` and `presentation`/`ui` start in the logged-in state.
- If none exists (or restore fails), the user lands on the login screen from slice 2.
- Closing and relaunching the app after a slice-2 login demonstrates this without a fresh login prompt.

**Kalium APIs involved**: `CoreLogic.getGlobalScope()` / `GlobalKaliumScope`'s account list, `CoreLogic.getSessionScope(userId)`.

**What to test**: Unit test for the `data`-layer "restore vs. require login" decision logic, faking "account present" vs. "no account" states — inject time/dispatchers rather than relying on real I/O timing, per AGENTS.md.

**Out of scope**: Multi-account switching (out of scope for v1 generally). Gracefully handling a corrupted/partial local DB — treat as "no session" for now and log it.

**Dependencies**: Slice 2 — something must have logged in and persisted an account to restore.

## Later slices (one-line each, to be expanded with the full template when picked up)

4. **Conversations list screen** — fetch and display conversations (`GetConversationsUseCase`/observe variant).
5. **Open a conversation** — show its recent messages (`GetRecentMessagesUseCase`).
6. **Send a text message** — `SendTextMessageUseCase`, appended to the open conversation's view.
7. **Live updates** — switch one-shot fetches to the Observe* flows so new messages/conversations appear without manual refresh.
8. **Self user display** — show the logged-in user's name/handle (`GetSelfUserUseCase`/observe variant).
9. **Logout** — invoke Kalium's `LogoutUseCase` (invalidates the session server-side and clears local client/session data, unlike `deleteSessionScope` alone, which only tears down the in-memory scope) and return to login.
10. *(stretch)* **Basic connectivity/error state** surfaced in the UI — still desktop/JVM-only, no protocol changes.
