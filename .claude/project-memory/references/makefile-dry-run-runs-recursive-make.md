---
name: "make -n runs recipe lines containing $(MAKE)"
description: "Dry-running dev actually launches the stack, because make executes recursive-make lines even under -n"
type: feedback
---
# make -n runs recipe lines containing $(MAKE)

GNU make executes any recipe line containing `$(MAKE)` even with `-n`, so `make -n dev` really starts the Temporal dev server, the worker and both Nuxt apps: the whole `dev` recipe is one shell chain that includes `$(MAKE) worker &`, and the `until temporal operator cluster health` loop then blocks forever.

**How to apply:** dry-run only targets whose recipes contain no `$(MAKE)` (`make -n app-down` is safe). To inspect the `dev` recipe, read the `Makefile` instead. If a dry run has already been fired, reclaim the processes with `pkill -f 'temporal server start-dev'`, `pkill -f 'spring-boot:run'` and `pkill -f 'nuxt dev'`.

Related: the `dev` recipe's endpoint print and `publish-endpoints` call sit **inside** the backslash-continued shell chain (`$(MAKE) -s endpoints; \`), not on their own `@`-prefixed recipe lines. The recipe's leading `trap 'kill 0' EXIT` is what reaps the background jobs, and it only covers commands in the same shell invocation — splitting those lines out would break the trap.
