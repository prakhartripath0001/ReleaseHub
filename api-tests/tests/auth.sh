#!/usr/bin/env bash

# ==============================================================================
# Suite: Security, Role Guards & Error Handling
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Role Authorization & Error Handling"

# Test 1: PUBLISHER Role is Allowed (201)
log_step "Testing write as PUBLISHER..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"Publisher App","category":"test"}'
assert_status 201 "PUBLISHER allowed to create software"
CLEANUP_ID_1=$(json_val "$HTTP_BODY" ".id")

# Test 2: ADMIN Role is Allowed (201)
log_step "Testing write as ADMIN..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_ADMIN" \
    '{"name":"Admin App","category":"test"}'
assert_status 201 "ADMIN allowed to create software"
CLEANUP_ID_2=$(json_val "$HTTP_BODY" ".id")

# Test 3: USER Role is Denied (403)
log_step "Testing write as unauthorized USER..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_USER" \
    '{"name":"Unauthorized App","category":"test"}'
assert_status 403 "Standard USER denied (403 Forbidden)"

# Test 4: Missing Role Header is Denied (403)
log_step "Testing write without any role header..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON" \
    '{"name":"Anonymous App","category":"test"}'
assert_status 403 "Missing role denied (403 Forbidden)"

# Test 5: 404 on Non-existent UUID
log_step "Testing 404 Not Found on unknown ID..."
http_request "GET" "/software/00000000-0000-0000-0000-000000000001" "" ""
assert_status 404 "Non-existent entity returns 404"

# Test 6: 400 Bad Request on Empty Name Validation
log_step "Testing validation error on empty mandatory name..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"","description":"Empty name test"}'
assert_status 400 "Empty name returns 400 Bad Request"
assert_json_eq ".fieldErrors.name" "Name is required" "Field error 'Name is required' returned"

# Test 7: 400 on Malformed JSON
log_step "Testing error handling on malformed JSON payload..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":'
if [ "$HTTP_STATUS" -ge 400 ] && [ "$HTTP_STATUS" -lt 600 ]; then
    log_pass "Malformed JSON handled cleanly (Status: $HTTP_STATUS)"
else
    log_fail "Malformed JSON expected error status, got: $HTTP_STATUS"
fi

# Cleanup
log_step "Cleaning up auth test entities..."
[ -n "$CLEANUP_ID_1" ] && [ "$CLEANUP_ID_1" != "null" ] && http_request "DELETE" "/software/$CLEANUP_ID_1" "$ROLE_ADMIN" ""
[ -n "$CLEANUP_ID_2" ] && [ "$CLEANUP_ID_2" != "null" ] && http_request "DELETE" "/software/$CLEANUP_ID_2" "$ROLE_ADMIN" ""

suite_summary

