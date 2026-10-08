# wire-desktop-prototype

A Compose Multiplatform desktop client prototype, built against Wire's Kalium SDK `logic` module for messaging, encryption, and backend communication. The UI is Kotlin/Compose for Desktop (JVM); business logic is delegated to Kalium rather than reimplemented in this repo.

@AGENTS.md

Follow AGENTS.md — it is the single source of truth for architecture, size limits, testing, and hygiene standards, for both writing and reviewing code in this repo.

## Working rules

- **One branch per task.** Never commit directly to `main`. Create a branch per unit of work and open a PR from it.
- **Small PRs.** Scope each PR to a single task or fix. Prefer several small, reviewable PRs over one large one.
- **Never commit secrets.** No API keys, tokens, credentials, or `local.properties`-style config. If something needs a secret, it belongs in an ignored local file or env var, never in source.
- **Build before opening a PR.** Run the Gradle build locally and confirm it succeeds before pushing and opening a PR. A PR should never be opened against a known-broken build.
- To read or search files, use your built-in Read, Grep and Glob tools rather than shell commands like cat, grep, find or ls.

## PR workflow

After opening a PR:

1. Run `./scripts/codex-review.sh` — it reviews the branch against the Code Review Rules in AGENTS.md, posts the result as a PR comment titled "Codex review", and saves it to `review.md`.
2. Read `review.md` and fix every valid P0 and P1 finding, then push.
3. Re-run the review and repeat until no P0 or P1 findings remain.
4. Report back which findings were fixed and which were disagreed with, and why.

Claude never posts or edits PR review comments itself — that's the script's job, using Codex's own output.

## Project structure

- Gradle/Kotlin project using the Compose Multiplatform Gradle plugin, desktop (JVM) target only for now.
- `src/main/kotlin` — application source, split into `ui`, `presentation`, and `data` packages per AGENTS.md.

## License

GPL-3.0 (see `LICENSE`).
