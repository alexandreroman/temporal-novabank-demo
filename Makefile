# Temporal NovaBank demo

.DEFAULT_GOAL := help

# Search attributes required by the AccountApplicationWorkflow, reused by all
# targets that start the Temporal dev server.
SEARCH_ATTRS := --search-attribute "ReviewStatus=Keyword" \
	--search-attribute "KycStatus=Keyword" \
	--search-attribute "ApplicantName=Text"

# compose.override.yaml (auto-merged by docker compose) may remap the published
# host ports so parallel Casper worktrees don't collide. It is the source of
# truth: when present, read the actual published ports straight from it so the
# banner, the containerized stack and the host-side `make dev` flow can never
# diverge. Otherwise fall back to the conventional defaults.
ifneq (,$(wildcard compose.override.yaml))
FRONTEND_PORT      := $(shell sed -nE 's/.*"([0-9]+):3000".*/\1/p' compose.override.yaml | head -n1)
BACKOFFICE_PORT    := $(shell sed -nE 's/.*"([0-9]+):3001".*/\1/p' compose.override.yaml | head -n1)
TEMPORAL_GRPC_PORT := $(shell sed -nE 's/.*"([0-9]+):7233".*/\1/p' compose.override.yaml | head -n1)
TEMPORAL_UI_PORT   := $(shell sed -nE 's/.*"([0-9]+):8233".*/\1/p' compose.override.yaml | head -n1)
# Point the host-side dev flow (worker, frontend, backoffice) at the remapped
# Temporal gRPC port.
TEMPORAL_ADDRESS   ?= localhost:$(TEMPORAL_GRPC_PORT)
export TEMPORAL_ADDRESS
else
FRONTEND_PORT      := 3000
BACKOFFICE_PORT    := 3001
TEMPORAL_GRPC_PORT := 7233
TEMPORAL_UI_PORT   := 8233
endif

# The workspace info panel mirrors `make endpoints`, so whichever command brought
# the app up or down leaves it telling the truth. The CLI is on PATH outside a
# workspace too, hence the CASPER_WORKSPACE_ID test alongside it, and `|| true`
# keeps a panel update from ever failing the target that asked for it.
in-casper-workspace = [ -n "$$CASPER_WORKSPACE_ID" ] && command -v casper >/dev/null 2>&1

define publish-endpoints
$(in-casper-workspace) && $(MAKE) -s endpoints | casper info set - >/dev/null || true
endef

define clear-endpoints
$(in-casper-workspace) && casper info clear >/dev/null || true
endef

.PHONY: help temporal worker worker-native e2e frontend backoffice dev \
	install install-frontend install-backoffice \
	app-up app-down app-logs worktree-ports check endpoints clean

help: ## Show this help
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# --- Development (host, hot-reload) ---

temporal: ## Start the Temporal dev server (CLI) with the required search attributes
	temporal server start-dev --port $(TEMPORAL_GRPC_PORT) --ui-port $(TEMPORAL_UI_PORT) $(SEARCH_ATTRS)

worker: ## Run the Java worker with hot-reload (Spring Boot devtools)
	cd worker && ./mvnw spring-boot:run

worker-native: ## Compile the worker into a GraalVM native binary (requires GraalVM as the active JDK)
	cd worker && ./mvnw -Pnative native:compile -DskipTests

e2e: ## Run the end-to-end test against an already-running stack (Temporal + worker must be up)
	./e2e/account-application-e2e.sh

frontend: install-frontend ## Run the frontend dev server with hot-reload (Nuxt)
	cd frontend && npm run dev -- --port $(FRONTEND_PORT)

backoffice: install-backoffice ## Run the backoffice dev server with hot-reload (Nuxt)
	cd backoffice && npm run dev -- --port $(BACKOFFICE_PORT)

dev: install ## Run the full dev stack on the host with hot-reload (Temporal + worker + frontend + backoffice)
	@trap 'kill 0' EXIT; \
	echo "Starting Temporal dev server..."; \
	temporal server start-dev --port $(TEMPORAL_GRPC_PORT) --ui-port $(TEMPORAL_UI_PORT) $(SEARCH_ATTRS) & \
	until temporal operator cluster health --address localhost:$(TEMPORAL_GRPC_PORT) >/dev/null 2>&1; do \
		echo "Waiting for Temporal to become healthy..."; \
		sleep 1; \
	done; \
	echo "Temporal is healthy. Starting worker, frontend and backoffice..."; \
	$(MAKE) worker & \
	$(MAKE) frontend & \
	$(MAKE) backoffice & \
	$(MAKE) -s endpoints; \
	$(call publish-endpoints); \
	wait

# --- Dependencies ---

install: install-frontend install-backoffice ## Install all Node dependencies (frontend + backoffice)

install-frontend: ## Install frontend Node dependencies
	cd frontend && npm install

install-backoffice: ## Install backoffice Node dependencies
	cd backoffice && npm install

# --- Containerized app (Docker Compose) ---

app-up: ## Build and start the full stack in containers (Docker Compose, foreground)
	@$(MAKE) -s endpoints
	@$(call publish-endpoints)
	docker compose up

app-down: ## Stop and remove the containerized stack
	docker compose down
	@$(call clear-endpoints)

app-logs: ## Tail logs from the containerized stack
	docker compose logs -f

# --- Casper worktree ---

worktree-ports: ## Remap published host ports off CASPER_PORT so parallel worktrees don't collide
	@if [ -n "$$CASPER_PORT" ]; then \
		printf 'services:\n  temporal:\n    ports: !override\n      - "%s:7233"\n      - "%s:8233"\n  frontend:\n    ports: !override\n      - "%s:3000"\n  backoffice:\n    ports: !override\n      - "%s:3001"\n' \
			$$((CASPER_PORT + 2)) $$((CASPER_PORT + 3)) "$$CASPER_PORT" $$((CASPER_PORT + 1)) > compose.override.yaml; \
		echo "Wrote compose.override.yaml (frontend=$$CASPER_PORT backoffice=$$((CASPER_PORT + 1)) temporal-grpc=$$((CASPER_PORT + 2)) temporal-ui=$$((CASPER_PORT + 3)))"; \
	else \
		echo "CASPER_PORT not set; skipping (using default ports)"; \
	fi

# --- Quality ---

check: install ## Build the worker and the two frontends to verify everything compiles
	cd worker && ./mvnw -q verify
	cd frontend && npm run build
	cd backoffice && npm run build

# --- Helpers ---

# Markdown on stdout, so the answer to "where is this app listening?" reads in a
# terminal and pipes straight into whatever renders it. The ports come from the
# compose.override.yaml readback block above, so they follow any CASPER_PORT remap.
# The columns are padded to fixed widths purely for the terminal: Markdown renderers
# ignore the extra spaces, so the same bytes serve both stdout and the info panel.
# printf cycles its format over the remaining operands, so one call emits every row.
endpoints: ## Print this worktree's published endpoints as Markdown
	@printf '%s\n\n' '# NovaBank'
	@printf '| %-15s | %-24s |\n' \
		'Service'         'Address' \
		'---------------' '------------------------' \
		'Frontend'        '<http://localhost:$(FRONTEND_PORT)>' \
		'Backoffice'      '<http://localhost:$(BACKOFFICE_PORT)>' \
		'Temporal Web UI' '<http://localhost:$(TEMPORAL_UI_PORT)>'
	@printf '\n'

# --- Cleanup ---

clean: ## Remove build artifacts and node_modules
	rm -rf worker/target
	rm -rf frontend/node_modules frontend/.nuxt frontend/.output
	rm -rf backoffice/node_modules backoffice/.nuxt backoffice/.output
