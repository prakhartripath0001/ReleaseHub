#!/usr/bin/env bash

# ==============================================================================
# Suite: Feedback, Transactional Outbox, and Analytics
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Feedback and Analytics APIs"

SOFTWARE_ID=""
VERSION_ID=""
TEST_TIMESTAMP="$(date +%s)"
USER_ID="f0000000-0000-4000-8000-$(printf '%012d' "$TEST_TIMESTAMP")"

cleanup_feedback_fixture() {
    if [ -n "$SOFTWARE_ID" ]; then
        http_request "DELETE" "/software/$SOFTWARE_ID" "$ROLE_ADMIN" "" "$BASE_URL"
    fi
}
trap cleanup_feedback_fixture EXIT

log_step "Checking Feedback Service at $FEEDBACK_BASE_URL..."
http_request "GET" "/actuator/health" "" "" "$FEEDBACK_BASE_URL"
assert_status 200 "Feedback service is healthy"

log_step "Creating catalog software and a version for feedback..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    "{\"name\":\"Feedback Manual Test $TEST_TIMESTAMP\",\"description\":\"Feedback API test\",\"category\":\"feedback-test\",\"tags\":[\"manual-test\"]}"
assert_status 201 "Create feedback test software"
SOFTWARE_ID=$(json_val "$HTTP_BODY" ".id")

if [ -z "$SOFTWARE_ID" ] || [ "$SOFTWARE_ID" = "null" ]; then
    log_fail "Could not retrieve the software fixture ID"
    suite_summary
    exit 1
fi

http_request "POST" "/software/$SOFTWARE_ID/versions" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"versionNumber":"1.0.0","os":"LINUX","releaseNotes":"Feedback test release","fileRef":"feedback-test/1.0.0"}'
assert_status 201 "Create feedback test version"
VERSION_ID=$(json_val "$HTTP_BODY" ".id")

log_step "Submitting valid feedback..."
http_request "POST" "/feedback" \
    "$CONTENT_TYPE_JSON" \
    "{\"versionId\":\"$VERSION_ID\",\"userId\":\"$USER_ID\",\"rating\":5,\"comment\":\"Excellent release\"}" \
    "$FEEDBACK_BASE_URL"
assert_status 201 "Submit feedback"
FEEDBACK_ID=$(json_val "$HTTP_BODY" ".id")
if [ -z "$FEEDBACK_ID" ] || [ "$FEEDBACK_ID" = "null" ]; then
    log_fail "Feedback response did not contain an ID"
fi

log_step "Checking feedback lookup by software..."
http_request "GET" "/feedback/$SOFTWARE_ID" "" "" "$FEEDBACK_BASE_URL"
assert_status 200 "List feedback for software"
assert_json_eq ".[0].rating" "5" "Feedback rating is returned"
assert_json_eq ".[0].versionId" "$VERSION_ID" "Feedback is linked to the requested version"

log_step "Checking duplicate feedback is rejected..."
http_request "POST" "/feedback" \
    "$CONTENT_TYPE_JSON" \
    "{\"versionId\":\"$VERSION_ID\",\"userId\":\"$USER_ID\",\"rating\":4,\"comment\":\"Duplicate submission\"}" \
    "$FEEDBACK_BASE_URL"
assert_status 409 "Duplicate feedback returns conflict"

log_step "Recording a download for analytics..."
http_request "GET" "/download/$VERSION_ID" "X-User-Id: $USER_ID" "" "$BASE_URL"
assert_status 200 "Record test download"

log_step "Waiting for asynchronous feedback and download analytics..."
REPORT_READY=0
for attempt in $(seq 1 15); do
    http_request "GET" "/reports/$SOFTWARE_ID" "" "" "$ANALYTICS_BASE_URL"
    if [ "$HTTP_STATUS" -eq 200 ] \
        && [ "$(json_val "$HTTP_BODY" ".feedbackCount")" = "1" ] \
        && [ "$(json_val "$HTTP_BODY" ".downloads")" = "1" ]; then
        REPORT_READY=1
        break
    fi
    sleep 1
done
assert_status 200 "Analytics report is available"
assert_json_eq ".feedbackCount" "1" "Analytics counts feedback once"
assert_json_eq ".avgRating" "5.00" "Analytics calculates the average rating"
assert_json_eq ".downloads" "1" "Analytics counts the download once"

if [ "$REPORT_READY" -ne 1 ]; then
    log_fail "Analytics did not reflect both events within 15 seconds"
fi

suite_summary
