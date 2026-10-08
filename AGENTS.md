# AGENTS.md

This is the single source of truth for how code is written and reviewed in this repository. It applies equally to humans and AI agents, and to authoring and reviewing code.

## Code Review Rules

Reviewers (human or AI) must check all of the following. Treat any violation of the architecture, size, testing, or hygiene rules as **P1** — it blocks the PR until fixed, not a "nice to have" follow-up.

Any change to `.github/workflows`, coverage thresholds, detekt config, or Konsist rules is **P1** and must be justified in the PR description.

### Architecture

The app is split into three layers, each its own package:

- **`ui`** — Compose screens and components only. No business logic, no direct access to `data`.
- **`presentation`** — state holders and ViewModels. No Compose UI code (no `@Composable` functions, no Compose Material/Foundation widgets).
- **`data`** — all Kalium access, wrapped behind interfaces. Nothing outside `data` imports Kalium directly.

Allowed dependency direction: `ui` → `presentation` → `data`. Never the other way round, and `ui` must not reach into `data` directly — it goes through `presentation`.

### Size limits

- Files under 300 lines.
- Functions under 40 lines.
- One main class per file.

### Testing

- Every `presentation` and `data` class has unit tests.
- Coverage of `presentation` and `data` stays at or above 80%.
- Bug fixes come with a test that fails without the fix.

### Hygiene

- No commented-out code.
- No TODOs without an explanation of what's deferred and why.
- Clear naming.
- No duplicated logic.
