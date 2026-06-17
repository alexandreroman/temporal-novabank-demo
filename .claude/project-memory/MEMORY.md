# Project Memory

> When a new decision **contradicts** an existing
> memory note, do NOT silently override it.
> Instead: surface the conflict, quote the
> existing memory, explain how the new decision
> differs, and ask for explicit confirmation
> before updating. **Do NOT take any action** —
> no tool calls, no file writes — until confirmed.

- [GraalVM native worker build](references/graalvm-native-worker.md) — native binary needs any GraalVM-based JDK; build/test entry points
- [Native build pitfalls](references/native-build-pitfalls.md) — changes that silently break the native image while the JVM build keeps working
- [Temporal SDK + Nitro production bundling fix](references/temporal-sdk-nitro-prod-bundling.md) — prod node-server needs nitro.externals.inline for @temporalio/* + protobufjs; dev mode hides the crash
