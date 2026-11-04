#!/usr/bin/env bash

# ==============================================================================
# Suite: Version Management APIs (Lifecycle & Sorting)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Version Management APIs"

# Step 0: Setup Parent Software
log_step "Creating parent software for version testing..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"IntelliJ IDEA","description":"Java IDE","category":"ide","tags":["java","ide"]}'
assert_status 201 "Parent software created"
SOFTWARE_ID=$(json_val "$HTTP_BODY" ".id")

# Test 1: Create Version 1.0.0
log_step "Creating version 1.0.0..."
http_request "POST" "/software/$SOFTWARE_ID/versions" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"versionNumber":"1.0.0","os":"LINUX","releaseNotes":"Initial stable release","fileRef":"storage/idea/1.0.0"}'
assert_status 201 "Create version 1.0.0"
VERSION_ID_1=$(json_val "$HTTP_BODY" ".id")

# Sleep 1s to guarantee distinct release timestamps
sleep 1

# Test 2: Create Version 2.0.0
log_step "Creating version 2.0.0..."
http_request "POST" "/software/$SOFTWARE_ID/versions" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"versionNumber":"2.0.0","os":"LINUX","releaseNotes":"Second major release","fileRef":"storage/idea/2.0.0"}'
assert_status 201 "Create version 2.0.0"
VERSION_ID_2=$(json_val "$HTTP_BODY" ".id")

# Test 3: List Versions (Verify Newest-First Ordering)
log_step "Verifying newest-first sort ordering..."
http_request "GET" "/software/$SOFTWARE_ID/versions" "" ""
assert_status 200 "List versions for software"
assert_json_eq ".[0].versionNumber" "2.0.0" "First element is newest version (2.0.0)"
assert_json_eq ".[1].versionNumber" "1.0.0" "Second element is previous version (1.0.0)"

# Test 4: Fetch Single Version by ID
log_step "Fetching version by ID: $VERSION_ID_1..."
http_request "GET" "/versions/$VERSION_ID_1" "" ""
assert_status 200 "Fetch single version by ID"
assert_json_eq ".versionNumber" "1.0.0" "Version number matches 1.0.0"
assert_json_eq ".softwareId" "$SOFTWARE_ID" "Linked softwareId matches parent"

# Test 5: Delete Version
log_step "Deleting version 2.0.0 ($VERSION_ID_2)..."
http_request "DELETE" "/versions/$VERSION_ID_2" \
    "$ROLE_PUBLISHER" ""
assert_status 204 "Delete version 2.0.0"

# Test 6: Verify Version Deletion returns 404
log_step "Verifying deleted version returns 404..."
http_request "GET" "/versions/$VERSION_ID_2" "" ""
assert_status 404 "Deleted version returns 404"

# Cleanup Parent
log_step "Cleaning up parent software..."
http_request "DELETE" "/software/$SOFTWARE_ID" "$ROLE_PUBLISHER" ""
assert_status 204 "Cleanup parent software"

suite_summary

