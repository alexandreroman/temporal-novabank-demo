---
name: "Casper info panel mirrors make endpoints"
description: "The Makefile's endpoints target is the single source of the app URLs, and every up/down target syncs it into the Casper info panel"
type: project
---
# Casper info panel mirrors make endpoints

`make endpoints` is the only place that spells out the demo's URLs. It prints Markdown on stdout (a `# NovaBank` heading plus a Service/Address table for the frontend, the backoffice and the Temporal Web UI), so the same output reads in a terminal and pipes straight into a renderer. Its ports come from the `compose.override.yaml` readback block at the top of the `Makefile`, so they follow any `CASPER_PORT` remap written by `make worktree-ports`.

Three macros near the top of the `Makefile` wire that output into the Casper workspace info panel:

- `in-casper-workspace` — guard testing `CASPER_WORKSPACE_ID` **and** `command -v casper`, because the CLI is on PATH outside a workspace too.
- `publish-endpoints` — `$(MAKE) -s endpoints | casper info set -`.
- `clear-endpoints` — `casper info clear`.

Both macros end in `|| true` so a panel update never fails the target that asked for it.

**How to apply:** any target that brings the app up (`dev`, `app-up`) prints `$(MAKE) -s endpoints` and then calls `publish-endpoints`; any target that brings it down (`app-down`) calls `clear-endpoints` last. Never hand-roll `echo "http://localhost:..."` lines in a recipe — add the URL to `endpoints` instead. New targets also need their name in the `.PHONY` list and a `## ` help comment, and section headers follow this file's `# --- Section ---` style (the `help` awk script does not parse `##@`).
