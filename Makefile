# Temporal NovaBank demo

.DEFAULT_GOAL := help

# Search attributes required by the AccountApplicationWorkflow, reused by all
# targets that start the Temporal dev server.
SEARCH_ATTRS := --search-attribute "ReviewStatus=Keyword" \
	--search-attribute "KycStatus=Keyword" \
	--search-attribute "ApplicantName=Text"

.PHONY: help temporal worker worker-native e2e frontend backoffice dev \
	install install-frontend install-backoffice \
	app-up app-down app-logs check clean

help: ## Show this help
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# --- Development (host, hot-reload) ---

temporal: ## Start the Temporal dev server (CLI) with the required search attributes
	temporal server start-dev $(SEARCH_ATTRS)

worker: ## Run the Java worker with hot-reload (Spring Boot devtools)
	cd worker && ./mvnw spring-boot:run

worker-native: ## Compile the worker into a GraalVM native binary (requires GraalVM as the active JDK)
	cd worker && ./mvnw -Pnative native:compile -DskipTests

e2e: ## Run the end-to-end test against an already-running stack (Temporal + worker must be up)
	./e2e/account-application-e2e.sh

frontend: install-frontend ## Run the frontend dev server with hot-reload (Nuxt)
	cd frontend && npm run dev

backoffice: install-backoffice ## Run the backoffice dev server with hot-reload (Nuxt)
	cd backoffice && npm run dev

dev: install ## Run the full dev stack on the host with hot-reload (Temporal + worker + frontend + backoffice)
	@trap 'kill 0' EXIT; \
	echo "Starting Temporal dev server..."; \
	temporal server start-dev $(SEARCH_ATTRS) & \
	until temporal operator cluster health >/dev/null 2>&1; do \
		echo "Waiting for Temporal to become healthy..."; \
		sleep 1; \
	done; \
	echo "Temporal is healthy. Starting worker, frontend and backoffice..."; \
	$(MAKE) worker & \
	$(MAKE) frontend & \
	$(MAKE) backoffice & \
	echo ""; \
	echo "  Frontend:    http://localhost:3000"; \
	echo "  Backoffice:  http://localhost:3001"; \
	echo "  Temporal UI: http://localhost:8233"; \
	echo ""; \
	wait

# --- Dependencies ---

install: install-frontend install-backoffice ## Install all Node dependencies (frontend + backoffice)

install-frontend: ## Install frontend Node dependencies
	cd frontend && npm install

install-backoffice: ## Install backoffice Node dependencies
	cd backoffice && npm install

# --- Containerized app (Docker Compose) ---

app-up: ## Build and start the full stack in containers (Docker Compose)
	docker compose up --build -d
	@echo ""
	@echo "  Frontend:    http://localhost:3000"
	@echo "  Backoffice:  http://localhost:3001"
	@echo "  Temporal UI: http://localhost:8233"

app-down: ## Stop and remove the containerized stack
	docker compose down

app-logs: ## Tail logs from the containerized stack
	docker compose logs -f

# --- Quality ---

check: install ## Build the worker and the two frontends to verify everything compiles
	cd worker && ./mvnw -q verify
	cd frontend && npm run build
	cd backoffice && npm run build

# --- Cleanup ---

clean: ## Remove build artifacts and node_modules
	rm -rf worker/target
	rm -rf frontend/node_modules frontend/.nuxt frontend/.output
	rm -rf backoffice/node_modules backoffice/.nuxt backoffice/.output
