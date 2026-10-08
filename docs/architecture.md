# Architecture notes from Kalium

Notes taken from `../kalium/README.md` ahead of integrating Kalium's `logic` module, for context on how the SDK we depend on is structured and built. Kalium has no `CONTRIBUTING.md` or `ARCHITECTURE.md` — this is drawn from its root README.

## Module layering

Kalium is split into many Gradle modules, with `logic` sitting at the top as the orchestration layer: it depends on `data`, `network`, `network-util`, `cryptography`, `persistence`, `calling`, `cells`, `backup`, `protobuf`, `util`, `common`, and `logger`. Everything below `logic` is an implementation detail of the SDK.

This matches the boundary already drawn in [AGENTS.md](../AGENTS.md): this repo's `data` layer should only ever import Kalium's `logic` module, never reach past it into `network`, `persistence`, `cryptography`, etc. directly. `logic` is the one supported entry point.

## Native dependencies

Kalium's crypto (`cryptography`, used transitively by `logic`) depends on native libraries — `libsodium`, `cryptobox-c`, `cryptobox4j`. Any Gradle task that touches that code path needs `-Djava.library.path=<path-to-native-libs>` on the JVM. This doesn't affect this repo's UI/presentation code, but it will matter once `data` actually exercises Kalium's crypto-backed APIs (directly, or in tests that don't fake the `logic` boundary).

## Detekt

Kalium enforces style with Detekt both as live IDE feedback (detekt IntelliJ plugin pointed at `detekt/detekt.yml`) and in CI (`./gradlew clean detekt`). We follow the same pattern — our `config/detekt/detekt.yml` is based on Kalium's, with our own size-limit overrides (see [ADR 0001](adr/0001-record-architecture-decisions.md) for the record-keeping convention; the detekt base-config choice itself was small enough not to need its own ADR).

## Not adopted

Kalium's config references a custom `WireRuleSet` (e.g. `EnforceSerializableFields`, `DocumentedPublicUseCases`) backed by a `detekt-rules` plugin jar built from their own module. We don't have that module here, so those rules are omitted from our detekt config rather than carried over as dead configuration.
