#!/usr/bin/env bash

# ==============================================================================
# ReleaseHub API Test Framework - Environment & Core Utilities
# ==============================================================================

# Target Host Configuration
export BASE_URL="${BASE_URL:-http://localhost:8083}"
export SEARCH_BASE_URL="${SEARCH_BASE_URL:-http://localhost:8084}"
export REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-10}"

# Default Roles / Headers
export ROLE_PUBLISHER="X-USER-ROLE: PUBLISHER"
export ROLE_ADMIN="X-USER-ROLE: ADMIN"
export ROLE_USER="X-USER-ROLE: USER"
export CONTENT_TYPE_JSON="Content-Type: application/json"

# ANSI Terminal Colors
BOLD='\033[1m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

# Test Metrics
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Loggers
log_header() {
    echo ""
    echo -e "${BOLD}${CYAN}==> $1${NC}"
}

log_step() {
    echo -e "  ${GRAY}•${NC} $1"
}

log_pass() {
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "  ${GREEN}✓ PASS:${NC} $1"
}

log_fail() {
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "  ${RED}✗ FAIL:${NC} $1"
    if [ -n "${HTTP_BODY:-}" ]; then
        echo -e "    ${GRAY}Response [${HTTP_STATUS:-0}]:${NC} ${HTTP_BODY}"
    fi
}

log_warn() {
    echo -e "  ${YELLOW}! WARN:${NC} $1"
}

# ==============================================================================
# HTTP Client Wrapper
# Sets: HTTP_STATUS, HTTP_BODY, HTTP_TIME_MS
# Usage: http_request METHOD ENDPOINT [HEADERS...] [BODY]
# ==============================================================================
http_request() {
    local method="$1"
    local endpoint="$2"
    local headers="${3:-}"
    local body="${4:-}"
    local base_url="${5:-$BASE_URL}"

    local temp_file
    temp_file=$(mktemp)

    local start_time
    start_time=$(python3 -c 'import time; print(int(time.time() * 1000))' 2>/dev/null || echo 0)

    # Build curl arguments
    local curl_cmd=(curl -s -S -w "\n%{http_code}" --max-time "$REQUEST_TIMEOUT" -X "$method" "$base_url$endpoint")

    if [ -n "$headers" ]; then
        # Parse space-delimited headers if array isn't provided
        while IFS= read -r header; do
            [ -n "$header" ] && curl_cmd+=(-H "$header")
        done <<< "$(echo -e "$headers" | tr ';' '\n')"
    fi

    if [ -n "$body" ]; then
        curl_cmd+=(-d "$body")
    fi

    # Execute
    local raw_output
    if ! raw_output=$("${curl_cmd[@]}" 2>"$temp_file"); then
        HTTP_STATUS=0
        HTTP_BODY="Curl Error: $(cat "$temp_file")"
        rm -f "$temp_file"
        return 1
    fi
    rm -f "$temp_file"

    local end_time
    end_time=$(python3 -c 'import time; print(int(time.time() * 1000))' 2>/dev/null || echo 0)
    HTTP_TIME_MS=$((end_time - start_time))

    # Split output into body and status code (last line)
    HTTP_STATUS=$(echo "$raw_output" | tail -n1)
    HTTP_BODY=$(echo "$raw_output" | sed '$d')
}

# ==============================================================================
# Assertion Helpers
# ==============================================================================
assert_status() {
    local expected="$1"
    local description="$2"

    if [ "$HTTP_STATUS" -eq "$expected" ]; then
        log_pass "$description (Status: $HTTP_STATUS, ${HTTP_TIME_MS}ms)"
        return 0
    else
        log_fail "$description (Expected: $expected, Received: $HTTP_STATUS)"
        return 1
    fi
}

# Extract JSON property by selector
json_val() {
    local json="$1"
    local selector="$2"

    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r "$selector" 2>/dev/null
    else
        python3 -c "import sys, json; data=json.loads(sys.stdin.read()); print(data$selector)" <<< "$json" 2>/dev/null
    fi
}

assert_json_eq() {
    local selector="$1"
    local expected="$2"
    local description="$3"

    local actual
    actual=$(json_val "$HTTP_BODY" "$selector")

    if [ "$actual" = "$expected" ]; then
        log_pass "$description [field $selector == '$expected']"
        return 0
    else
        log_fail "$description [Expected $selector to be '$expected', got '$actual']"
        return 1
    fi
}

# ==============================================================================
# Suite Summary
# ==============================================================================
suite_summary() {
    echo ""
    echo -e "${GRAY}------------------------------------------------------------${NC}"
    if [ "$FAILED_TESTS" -eq 0 ]; then
        echo -e "${GREEN}${BOLD}✔ All $PASSED_TESTS tests passed!${NC}"
    else
        echo -e "${RED}${BOLD}✖ $FAILED_TESTS of $TOTAL_TESTS tests failed!${NC}"
        return 1
    fi
}
