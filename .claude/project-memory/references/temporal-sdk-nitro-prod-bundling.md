---
name: "Temporal SDK + Nitro production bundling fix"
description: "Nuxt prod node-server crashes on @temporalio/* extensionless ESM imports; fix via nitro.externals.inline"
type: project
---

# Temporal SDK + Nitro production bundling fix

The Temporal TypeScript SDK (`@temporalio/*`) and `protobufjs` are CommonJS
packages with no `exports` map that use extensionless subpath imports (e.g.
`@temporalio/common/lib/errors`). Nuxt/Nitro's production node-server output
(`node .output/server/index.mjs`) crashes at runtime with `ERR_MODULE_NOT_FOUND`
because Node's strict ESM resolver does not add extensions for bare-specifier
subpaths. This is **pre-existing and version-independent** (reproduced on
`@temporalio/client` 1.17.2, 1.17.4 and 1.18.1, and on both Node 22 and Node 24).

The required fix lives in `nuxt.config.ts` of both `frontend/` and `backoffice/`:
set `nitro.externals.inline` to the full consistent set:
`@temporalio/client`, `@temporalio/common`, `@temporalio/proto`,
`@temporalio/activity`, `@temporalio/envconfig`, `protobufjs`, and all
`@protobufjs/*` helpers (`aspromise`, `base64`, `codegen`, `eventemitter`,
`fetch`, `float`, `path`, `pool`, `utf8`). `@grpc/grpc-js` and `long` stay
external (they are resolvable root imports).

**Why:** Partial inlining fails. `@temporalio/common` and `@temporalio/proto`
must be inlined together or `common` reads an undefined proto namespace
(`Cannot read properties of undefined (reading 'api')`). `protobufjs` must be
inlined together with its `@protobufjs/*` helpers or esbuild's CJS interop
breaks its lazy `util.pool` initialization (`util.pool is not a function`).

**How to apply:** Keep the `nitro.externals.inline` list intact when editing
either `nuxt.config.ts`. Nitro 2.13.4 matches string entries in that list but
NOT RegExp entries, so use explicit package-name strings. Always validate the
production server itself (`node .output/server/index.mjs`, or `docker compose`),
never just `nuxt build` or `npm run dev` — dev mode (Vite) resolves leniently
and `/healthz` is a static route, so both hide this failure. A fast local check:
`TEMPORAL_ADDRESS=localhost:7233 NITRO_PORT=<port> node .output/server/index.mjs`
then `POST /api/applications/start`.
