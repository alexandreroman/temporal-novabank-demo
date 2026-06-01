# Temporal NovaBank demo

Multi-step account opening form for **NovaBank** (fictional bank), powered by Temporal durable execution as sole source of state (no database).

See [README.md](README.md) for full documentation.

## Rules

- All code, comments, and text must be in English only.
- ALWAYS use the `temporal` CLI to debug workflows and retrieve Temporal-related details (workflow state, history, search attributes, etc.) instead of guessing or relying on memory.
- NEVER use compound bash commands (`&&`, `;`). Use separate Bash tool calls instead.
- Use `docker compose` (with a space) instead of `docker-compose` (hyphenated) in all commands and documentation.

## Agents

Use the following agents (from the [skillbox](https://github.com/alexandreroman/skillbox) plugin) for all code tasks:

- **code-writer** — for ANY task that writes, modifies, or refactors code, no matter how small (renames, find-and-replace, single-line edits, refactoring, new code). Never use the Edit or Write tools directly on source files — always delegate to this agent.
- **code-reviewer** — for read-only code review before merging or when investigating issues.

## Memory

At the start of every conversation, read `.claude/project-memory/MEMORY.md` to load project context from previous conversations.

Use the **project-memory** skill (from the [skillbox](https://github.com/alexandreroman/skillbox) plugin) proactively — without being asked — whenever the conversation reveals project decisions, deadlines, external references, workflow preferences, or corrective feedback worth persisting across conversations.

**Important:** Persist project context only via the **project-memory** skill (`.claude/project-memory/`). Never use the built-in auto-memory (`~/.claude/projects/.../memory/`) — it is local and not shared with the team.

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
