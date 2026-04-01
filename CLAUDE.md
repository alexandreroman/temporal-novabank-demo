# Temporal NovaBank demo

Multi-step account opening form for **NovaBank** (fictional bank), powered by Temporal durable execution as sole source of state (no database).

## Rules

- All code, comments, and text must be in English only.
- ALWAYS use the `code-writer` agent for ANY code modification, no matter how small (including simple renames, find-and-replace, single-line edits, refactoring, and new code).
- ALWAYS use the `temporal` CLI to debug workflows and retrieve Temporal-related details (workflow state, history, search attributes, etc.) instead of guessing or relying on memory.
- NEVER use compound bash commands (`&&`, `;`). Use separate Bash tool calls instead.
- Use `docker-compose` (hyphenated) instead of `docker compose` in all commands and documentation.

## Architecture

Monorepo — 3 independent components coordinated only through Temporal:

| Component | Stack | URL |
|---|---|---|
| **frontend/** | Vue.js, Temporal TS SDK | `localhost:3000` |
| **backoffice/** | Vue.js, Temporal TS SDK | `localhost:3001` |
| **worker/** | Java, Spring Boot, Temporal Java SDK | — |

## Workflow

`AccountApplicationWorkflow` — entity workflow holding all form state durably.

- **Signals** — `submitPage1–3()`, `submitFinalForm()`, `submitReviewDecision()`, `goToPage()`
- **Query** — `getFormState()` → current page, status, form data, KYC info
- **Child workflow** — KYC verification runs in background during form filling
- **Timer** — 3-min abandonment timeout (resettable on form activity)
- **Human-in-the-loop** — compliance officer approves/rejects via backoffice

Search attributes: `ReviewStatus` (Keyword), `KycStatus` (Keyword), `ApplicantName` (Text).

## Local Development

```bash
temporal server start-dev \
  --search-attribute "ReviewStatus=Keyword" \
  --search-attribute "KycStatus=Keyword" \
  --search-attribute "ApplicantName=Text"

cd worker && ./mvnw spring-boot:run        # Terminal 2
cd frontend && npm install && npm run dev   # Terminal 3
cd backoffice && npm install && npm run dev # Terminal 4
```

Temporal UI: http://localhost:8233

## Debugging

- `temporal workflow show|query|signal|stack` to inspect workflows via CLI.
- Each form page has an "Auto-fill demo data" button for quick testing.

## Brand

NovaBank — `--ink: #0f1923`, `--gold: #c9a84c`, `--paper: #f5f2ec` — Playfair Display (headings) + DM Sans (body).
