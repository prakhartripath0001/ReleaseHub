#!/usr/bin/env bash

# ==============================================================================
# Suite: Software Catalog APIs (CRUD, Filters, Pagination)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Software Catalog APIs"

# Test 1: Create Software Entry
log_step "Creating software entry 'Visual Studio Code'..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"Visual Studio Code","description":"Source code editor","category":"tools","tags":["editor","dev-tools"]}'

assert_status 201 "Create software entry"
SOFTWARE_ID=$(json_val "$HTTP_BODY" ".id")

if [ -z "$SOFTWARE_ID" ] || [ "$SOFTWARE_ID" = "null" ]; then
    log_fail "Failed to retrieve generated software ID"
    exit 1
fi
log_step "Created Software ID: $SOFTWARE_ID"

# Test 2: Fetch by ID
log_step "Fetching software by ID: $SOFTWARE_ID..."
http_request "GET" "/software/$SOFTWARE_ID" "" ""
assert_status 200 "Fetch software by ID"
assert_json_eq ".name" "Visual Studio Code" "Validate entity name"
assert_json_eq ".category" "tools" "Validate category"

# Test 3: List all with pagination
log_step "Listing all software with pagination (page=0, size=5)..."
http_request "GET" "/software?page=0&size=5" "" ""
assert_status 200 "List all software"
assert_json_eq ".size" "5" "Pagination size matches query"

# Test 4: Category Filtering
log_step "Filtering software by category=tools..."
http_request "GET" "/software?category=tools" "" ""
assert_status 200 "Filter by category=tools"

# Test 5: OS Filtering
log_step "Filtering software by os=LINUX..."
http_request "GET" "/software?os=LINUX" "" ""
assert_status 200 "Filter by os=LINUX"

# Test 6: Update Software Entry
log_step "Updating software entry..."
http_request "PUT" "/software/$SOFTWARE_ID" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"name":"Visual Studio Code Updated","description":"Updated description","category":"tools","tags":["editor","coding"]}'

assert_status 200 "Update software entry"
assert_json_eq ".name" "Visual Studio Code Updated" "Validate updated name"

# Test 7: Delete Software Entry
log_step "Deleting software entry..."
http_request "DELETE" "/software/$SOFTWARE_ID" \
    "$ROLE_PUBLISHER" ""
assert_status 204 "Delete software entry"

# Test 8: Verify Deletion returns 404
log_step "Verifying software entry is deleted..."
http_request "GET" "/software/$SOFTWARE_ID" "" ""
assert_status 404 "Deleted software returns 404 Not Found"

suite_summary

