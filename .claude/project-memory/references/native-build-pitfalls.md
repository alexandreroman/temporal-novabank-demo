---
name: "Native build pitfalls (silent breakage)"
description: "Worker changes that keep the JVM build working but silently break the GraalVM native image"
type: feedback
---
# Native build pitfalls (silent breakage)

Some worker changes keep the JVM build working but break the GraalVM native image; only `make e2e` against the native binary catches them. The two confirmed traps: (1) keep the explicit `spring.temporal.workers` config — do NOT revert to `workers-auto-discovery`; (2) never put `@RegisterReflection` and `@RegisterReflectionForBinding` on the same class.

**Why:** `temporal-spring-boot-autoconfigure` ships no native-image metadata (sdk-java#2064). Native images can't do runtime classpath scanning, so `workers-auto-discovery` finds no `@WorkflowImpl` (only the activity poller starts, workflow queries time out). And Spring AOT silently drops one of two co-located `@Reflective` annotations, which lost the Jackson payload binding hints and broke deserialization at runtime.

**How to apply:** after any worker change touching worker registration, payload models, or native reflection/proxy hints, run `make e2e` on the native binary before claiming it works. When changing worker registration, keep the explicit `workers` list; keep binding and reflection annotations on separate carrier classes.
