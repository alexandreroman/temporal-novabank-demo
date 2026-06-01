---
name: "GraalVM native worker build"
description: "The worker can be built as a GraalVM native binary; required toolchain and entry points"
type: project
---
# GraalVM native worker build

The Spring Boot worker can be compiled to a GraalVM native binary (added 2026-06-01). Native builds just need *any* GraalVM-based JDK (with `native-image`) as the active JDK — Oracle GraalVM, GraalVM CE, Liberica NIK, Mandrel, etc. all work. No specific distribution is required; whichever is convenient is fine.

**Why:** a plain JDK has no `native-image`, so the native profile fails unless a GraalVM distribution is the active JDK. The config is distribution-agnostic. Native support was validated end-to-end on both the native binary and the JVM jar (the dev machine happened to use Liberica NIK `25.0.3.r25-nik` via sdkman).

**How to apply:** make any GraalVM distribution the active JDK (e.g. via sdkman: `sdk use java <some-graal-or-nik-id>`), then `make worker-native` to compile `worker/target/novabank-worker`. Verify with `make e2e` (a flavor-agnostic black-box test that assumes Temporal + a worker are already running; its `COMPLETION_TIMEOUT` defaults to 300s because the demo KYC activity fails ~60% at random and retries with backoff). See "Native build pitfalls".
