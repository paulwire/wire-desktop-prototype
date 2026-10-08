# AGENTS.md

This is the single source of truth for how code is written and reviewed in this repository. It applies equally to humans and AI agents, and to authoring and reviewing code.

## Agent workflow

- **Small, low-risk changes can be made directly.** A bug fix, a small refactor confined to one area, a doc tweak, a dependency bump with no API impact — just do it.
- **Broad, multi-file, or debatable changes must be proposed first.** If a change touches many files, changes architecture or public interfaces, or involves a judgment call reasonable people could disagree on, propose the approach and wait for confirmation before implementing.

## Conventions

- **Test names** follow `givenX_whenY_thenZ`.
- **Tests verify real behaviour or risk.** A test must never exist solely to raise a coverage number — if it doesn't assert something that could actually fail, delete it.
- **Inject time and coroutine dispatchers** into testable logic instead of calling system time (`System.currentTimeMillis()`, `Clock.System.now()`) or global dispatchers (`Dispatchers.IO`, `Dispatchers.Default`) directly.
- **Use cases are interfaces** with explicit constructor injection — no service-locator or singleton lookups.
- **Code lives in the narrowest layer that can own it.** Don't lift logic into `presentation` or `ui` if `data` can own it, and vice versa.
- **Every Kotlin file starts with the GPL-3.0 licence header.**
- **TODOs reference an issue number** (e.g. `// TODO(WIRE-1234): ...`), in addition to explaining what's deferred and why.
- **UI colours come only from the app theme** — no hardcoded `Color(...)` literals in `ui`.
- **UI elements have content descriptions** for accessibility.
- **Commit messages have a subject plus a body** explaining what changed and why.
- **New dependencies need a licence check for GPL compatibility** before being added.

## Code Review Rules

Reviewers (human or AI) must check all of the following. Treat any violation of the architecture, size, testing, or hygiene rules as **P1** — it blocks the PR until fixed, not a "nice to have" follow-up.

Any change to `.github/workflows`, coverage thresholds, detekt config, or Konsist rules is **P1** and must be justified in the PR description.

Security and accessibility problems are always **P1**, regardless of which section below they fall under.

Reviewers should not comment on anything detekt or other formatting/static-analysis tools already catch — that's noise, not review.

### Architecture

The app is split into three layers, each its own package:

- **`ui`** — Compose screens and components only. No business logic, no direct access to `data`.
- **`presentation`** — state holders and ViewModels. No Compose UI code (no `@Composable` functions, no Compose Material/Foundation widgets).
- **`data`** — all Kalium access, wrapped behind interfaces. Nothing outside `data` imports Kalium directly.

Allowed dependency direction: `ui` → `presentation` → `data`. Never the other way round, and `ui` must not reach into `data` directly — it goes through `presentation`.

### Size limits

Applies to Kotlin source files (`src/`); it does not apply to vendored or generated configuration, such as `config/detekt/detekt.yml`, which is adopted wholesale from a tool or another project rather than authored here.

- Files under 300 lines.
- Functions under 40 lines.
- One main class per file.

### Testing

Applies to application code (`src/`); root build-script and tooling fixes (e.g. `build.gradle.kts` task configuration) are exempt from the regression-test requirement below — verify those manually and explain the verification in the PR description instead.

- Every `presentation` and `data` class has unit tests.
- Coverage of `presentation` and `data` stays at or above 80%.
- Bug fixes come with a test that fails without the fix.

### Hygiene

- No commented-out code.
- No TODOs without an explanation of what's deferred and why.
- Clear naming.
- No duplicated logic.
