#!/usr/bin/env bash

# ==============================================================================
# Suite: Download Tracking & URL Resolution APIs
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Download Tracking APIs"

# Step 0: Setup Parent Software and Version
log_step "Creating parent software and release for download test..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"Docker Desktop","description":"Container runtime","category":"devops","tags":["docker"]}'
assert_status 201 "Parent software created"
SOFTWARE_ID=$(json_val "$HTTP_BODY" ".id")

FILE_REF="https://cdn.example.com/docker/4.25.0.dmg"
http_request "POST" "/software/$SOFTWARE_ID/versions" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    "{\"versionNumber\":\"4.25.0\",\"os\":\"macOS\",\"fileRef\":\"$FILE_REF\"}"
assert_status 201 "Release version created"
VERSION_ID=$(json_val "$HTTP_BODY" ".id")

# Test 1: Trigger Download with User Tracking Header
USER_ID="usr-test-4829"
log_step "Triggering download for version $VERSION_ID with User $USER_ID..."
http_request "GET" "/download/$VERSION_ID" \
    "X-User-Id: $USER_ID" ""

assert_status 200 "Trigger download endpoint"
assert_json_eq ".versionId" "$VERSION_ID" "Returned versionId matches requested"
assert_json_eq ".downloadUrl" "$FILE_REF" "Returned downloadUrl matches version fileRef"
assert_json_eq ".message" "Download recorded successfully" "Confirmation message verified"

# Test 2: MySQL Verification (if Docker container 'ReleaseHub' is running)
log_step "Checking MySQL for recorded download event..."
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "ReleaseHub"; then
    QUERY="SELECT count(*) FROM download_event WHERE version_id='$VERSION_ID' AND user_id='$USER_ID';"
    COUNT=$(docker exec ReleaseHub mysql -uroot -prelease@123 releasehub -N -s -e "$QUERY" 2>/dev/null || echo "0")
    if [ "$COUNT" -ge 1 ]; then
        log_pass "Verified download event persisted in MySQL (count: $COUNT)"
    else
        log_fail "Download event not persisted in MySQL"
    fi
else
    log_warn "Skipped direct MySQL table inspection (Docker container not accessible in current shell)"
fi

# Cleanup
log_step "Cleaning up test data..."
http_request "DELETE" "/software/$SOFTWARE_ID" "$ROLE_PUBLISHER" ""
assert_status 204 "Cleanup parent software"

suite_summary

