---
name: "Nuxt upgrades require a matching vite override"
description: "The vite entry in the overrides block must satisfy the range @nuxt/vite-builder asks for, in both Nuxt apps at once"
type: feedback
---
# Nuxt upgrades require a matching vite override

`frontend/package.json` and `backoffice/package.json` are byte-identical dependency manifests, and both pin `vite` in their `overrides` block. Any `nuxt` version bump has to move that `vite` override to a range satisfying what the new `@nuxt/vite-builder` depends on, and both files must be edited together so they stay identical. Known-good pairing: `nuxt ^4.5.1` with `vite ^8.1.5`.

**Why:** npm applies `overrides` to the whole tree, so the pinned range wins over the transitive requirement instead of being reconciled with it. A stale pin resolves an incompatible vite and the app fails at build time with `SyntaxError: The requested module 'vite' does not provide an export named 'transformWithOxc'` — a runtime-only symptom that no version-range warning surfaces during editing.

**How to apply:** when bumping `nuxt`, check the `vite` range required by the target release's `@nuxt/vite-builder` (`npm view @nuxt/vite-builder@<version> dependencies`) and update the `vite` override in the same commit, in both apps. Leave the other override entries (protobufjs, uuid, ws, simple-git, @grpc/grpc-js, esbuild, launch-editor, shell-quote) alone. Regenerate both lockfiles with `npm install` and commit them alongside the manifests — `make check` only catches this at `nuxt build` time, so never bump `nuxt` without running it.
