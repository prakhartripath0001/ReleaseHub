#!/usr/bin/env bash

# ==============================================================================
# ReleaseHub API Test Suite Orchestrator
# Usage:
#   ./run-tests.sh              # Runs all test suites
#   ./run-tests.sh software     # Runs only the software suite
#   ./run-tests.sh search       # Runs only the search suite
#   ./run-tests.sh feedback     # Runs feedback and analytics suites
#   ./run-tests.sh versions     # Runs only the versions suite
#   ./run-tests.sh downloads    # Runs only the downloads suite
#   ./run-tests.sh auth         # Runs only the auth & errors suite
# ==============================================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/config/env.sh"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found at $CONFIG_FILE"
    exit 1
fi

source "$CONFIG_FILE"

# Pre-flight check: Is the backend server running?
echo -e "${BOLD}${BLUE}Checking connectivity to $BASE_URL...${NC}"
if ! curl -s --max-time 3 "$BASE_URL/actuator/health" >/dev/null 2>&1 && ! curl -s --max-time 3 "$BASE_URL/software" >/dev/null 2>&1; then
    echo -e "${RED}${BOLD}Error: Cannot connect to Catalog Service at $BASE_URL${NC}"
    echo -e "Make sure the Spring Boot service is started: ${YELLOW}mvn spring-boot:run${NC}"
    echo -e "Or specify a custom URL: ${YELLOW}BASE_URL=\"http://localhost:8083\" ./run-tests.sh${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to $BASE_URL successfully!${NC}"
echo ""

START_TIME=$(date +%s)
FAILED_SUITES=0
TOTAL_SUITES=0

run_suite() {
    local suite_name="$1"
    local suite_script="$SCRIPT_DIR/tests/${suite_name}.sh"

    TOTAL_SUITES=$((TOTAL_SUITES + 1))

    if [ ! -f "$suite_script" ]; then
        echo -e "${RED}Suite '$suite_name' not found at $suite_script${NC}"
        FAILED_SUITES=$((FAILED_SUITES + 1))
        return
    fi

    if bash "$suite_script"; then
        echo -e "${GREEN}✓ Suite '${suite_name}' completed successfully.${NC}"
    else
        echo -e "${RED}✖ Suite '${suite_name}' encountered failures.${NC}"
        FAILED_SUITES=$((FAILED_SUITES + 1))
    fi
}

TARGET_SUITE="${1:-all}"

case "$TARGET_SUITE" in
    software|software.sh)
        run_suite "software"
        ;;
    search|search.sh)
        run_suite "search"
        ;;
    feedback|feedback.sh)
        run_suite "feedback"
        ;;
    versions|version|versions.sh)
        run_suite "versions"
        ;;
    downloads|download|downloads.sh)
        run_suite "downloads"
        ;;
    auth|auth.sh|security)
        run_suite "auth"
        ;;
    all)
        echo "============================================================"
        echo -e "${BOLD} Running All ReleaseHub API Test Suites${NC}"
        echo "============================================================"
        run_suite "software"
        run_suite "search"
        run_suite "feedback"
        run_suite "versions"
        run_suite "downloads"
        run_suite "auth"
        ;;
    *)
        echo -e "${RED}Unknown suite: $TARGET_SUITE${NC}"
        echo "Available suites: software, search, feedback, versions, downloads, auth, all"
        exit 1
        ;;
esac

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "============================================================"
echo -e "${BOLD} Test Run Summary (${DURATION}s)${NC}"
echo "============================================================"

if [ "$FAILED_SUITES" -eq 0 ]; then
    echo -e "${GREEN}${BOLD} ALL $TOTAL_SUITES SUITE(S) PASSED!${NC}"
    exit 0
else
    echo -e "${RED}${BOLD} $FAILED_SUITES OF $TOTAL_SUITES SUITE(S) FAILED!${NC}"
    exit 1
fi
