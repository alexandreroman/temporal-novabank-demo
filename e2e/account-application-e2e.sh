#!/usr/bin/env bash
#
# End-to-end test for the Temporal NovaBank worker.
#
# Black-box verification against an ALREADY-RUNNING stack: it assumes a Temporal
# service and a worker (started by any means — JVM, native binary, container, ...)
# are already up and connected. It does not start, configure or stop anything;
# it only drives the AccountApplicationWorkflow and asserts it completes with
# status "Approved".
#
# Exercises workflow start, signals, a query, the KYC child workflow, activities
# and the final result.
#
# Configuration via environment:
#   TEMPORAL_ADDRESS    Temporal frontend address (default: localhost:7233)
#   TASK_QUEUE          Worker task queue          (default: novabank)
#   COMPLETION_TIMEOUT  Seconds to wait for the workflow to complete (default: 300)
#
set -euo pipefail

export TEMPORAL_ADDRESS="${TEMPORAL_ADDRESS:-localhost:7233}"
TASK_QUEUE="${TASK_QUEUE:-novabank}"
COMPLETION_TIMEOUT="${COMPLETION_TIMEOUT:-300}"
SUFFIX="${RANDOM}-$$"
WID="acct-e2e-${SUFFIX}"
APP_ID="app-${SUFFIX}"

step() { echo "==> $*"; }
fail() {
    echo "ERROR: $*" >&2
    exit 1
}

# Poll a command until it succeeds (exit 0) or the timeout elapses.
# Usage: wait_for <timeout-seconds> <command...>
wait_for() {
    local timeout="$1"
    shift
    local elapsed=0
    while ! "$@" >/dev/null 2>&1; do
        if [[ "$elapsed" -ge "$timeout" ]]; then
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    return 0
}

require_cli() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "ERROR: required CLI '$1' is not on PATH." >&2
        exit 1
    }
}

require_cli temporal
require_cli jq

# --- Preconditions: the stack must already be running ----------------------

step "Checking Temporal is reachable at $TEMPORAL_ADDRESS"
if ! temporal operator cluster health >/dev/null 2>&1; then
    fail "Temporal is not reachable at $TEMPORAL_ADDRESS. Start the stack first (e.g. 'make temporal' and run the worker)."
fi

# Confirm a worker is polling BOTH a workflow-type and an activity-type task on
# the task queue. This is how we verify "the app is up and connected".
has_poller() {
    local queue_type="$1" count
    count="$(temporal task-queue describe \
        --task-queue "$TASK_QUEUE" \
        --task-queue-type "$queue_type" \
        -o json 2>/dev/null | jq '.pollers | length')"
    [[ "$count" =~ ^[0-9]+$ && "$count" -gt 0 ]]
}

step "Checking a worker is polling task queue '$TASK_QUEUE'"
if ! wait_for 15 has_poller workflow; then
    fail "no worker is polling a workflow task on '$TASK_QUEUE'. Is the worker running and connected to $TEMPORAL_ADDRESS?"
fi
if ! wait_for 15 has_poller activity; then
    fail "no worker is polling an activity task on '$TASK_QUEUE'. Is the worker running and connected to $TEMPORAL_ADDRESS?"
fi

# --- Drive the workflow ----------------------------------------------------

step "Starting workflow $WID (application $APP_ID)"
temporal workflow start \
    --task-queue "$TASK_QUEUE" \
    --type AccountApplicationWorkflow \
    --workflow-id "$WID" \
    --input "\"$APP_ID\""

step "Signal submitPage1 (personal info)"
temporal workflow signal \
    --workflow-id "$WID" \
    --name submitPage1 \
    --input '{"firstName":"Jane","lastName":"Doe","dateOfBirth":"1990-05-15","nationality":"French","idType":"Passport","idNumber":"X1234567","email":"jane.doe@example.com","phone":"+33123456789"}'

step "Query getFormState and assert page advanced to 2 with KYC started"
sleep 2
QUERY_OUT="$(temporal workflow query --workflow-id "$WID" --type getFormState -o json)"
QUERY_PAGE="$(echo "$QUERY_OUT" | jq -r '.queryResult[0].currentPage // empty')"
KYC_STATUS="$(echo "$QUERY_OUT" | jq -r '.queryResult[0].kyc.status // empty')"

if [[ "$QUERY_PAGE" != "2" ]] && [[ "$QUERY_OUT" != *'"currentPage": 2'* ]]; then
    echo "Query output:" >&2
    echo "$QUERY_OUT" >&2
    fail "expected currentPage == 2 after submitPage1"
fi
if [[ -z "$KYC_STATUS" ]]; then
    echo "Query output:" >&2
    echo "$QUERY_OUT" >&2
    fail "expected a non-empty KYC status (child workflow should have started)"
fi
step "Form is on page 2, KYC status: $KYC_STATUS"

step "Signal submitPage2 (address)"
temporal workflow signal \
    --workflow-id "$WID" \
    --name submitPage2 \
    --input '{"street":"1 Rue de la Paix","postalCode":"75002","city":"Paris","stateProvince":"Ile-de-France","country":"France"}'

step "Signal submitPage3 (financial)"
temporal workflow signal \
    --workflow-id "$WID" \
    --name submitPage3 \
    --input '{"employmentType":"Employed","employer":"ACME Corp","monthlyIncome":5000,"monthlyExpenses":2000}'

step "Signal submitFinalForm"
temporal workflow signal \
    --workflow-id "$WID" \
    --name submitFinalForm

step "Signal submitReviewDecision (Approved)"
temporal workflow signal \
    --workflow-id "$WID" \
    --name submitReviewDecision \
    --input '{"outcome":"Approved","reason":"All checks passed"}'

# --- Wait for completion ---------------------------------------------------

step "Waiting for the workflow to complete (timeout ${COMPLETION_TIMEOUT}s)"
STATUS=""
elapsed=0
while true; do
    STATUS="$(temporal workflow describe --workflow-id "$WID" -o json \
        | jq -r '.workflowExecutionInfo.status')"
    case "$STATUS" in
        WORKFLOW_EXECUTION_STATUS_COMPLETED)
            break
            ;;
        WORKFLOW_EXECUTION_STATUS_FAILED | \
        WORKFLOW_EXECUTION_STATUS_TERMINATED | \
        WORKFLOW_EXECUTION_STATUS_TIMED_OUT | \
        WORKFLOW_EXECUTION_STATUS_CANCELED)
            fail "workflow reached terminal status $STATUS before completing"
            ;;
    esac
    if [[ "$elapsed" -ge "$COMPLETION_TIMEOUT" ]]; then
        break
    fi
    sleep 3
    elapsed=$((elapsed + 3))
done

if [[ "$STATUS" != "WORKFLOW_EXECUTION_STATUS_COMPLETED" ]]; then
    fail "workflow did not complete within ${COMPLETION_TIMEOUT}s (last status: $STATUS). The demo KYC activity fails randomly and retries with backoff; raise COMPLETION_TIMEOUT if needed."
fi

# --- Assert the result -----------------------------------------------------

step "Fetching the workflow result"
RESULT_STATUS="$(temporal workflow result --workflow-id "$WID" -o json | jq -r '.result.status')"
if [[ "$RESULT_STATUS" != "Approved" ]]; then
    fail "expected result status 'Approved' but got '$RESULT_STATUS'"
fi

echo "E2E PASSED: worker completed AccountApplicationWorkflow with status Approved"
exit 0
