#!/usr/bin/env bash

# ==============================================================================
# Suite: Elasticsearch Search, Facets, Index Updates, and Reindexing
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config/env.sh"

log_header "Testing: Search APIs"

SOFTWARE_ID=""
SEARCH_TIMESTAMP="$(date +%s)"
SEARCH_NAME="ReleaseHub Search Manual $SEARCH_TIMESTAMP"
SEARCH_QUERY="ReleaseHub%20Search%20Manual%20$SEARCH_TIMESTAMP"
SEARCH_CATEGORY="search-manual-test"

cleanup_search_fixture() {
    if [ -n "$SOFTWARE_ID" ]; then
        http_request "DELETE" "/software/$SOFTWARE_ID" "$ROLE_ADMIN" "" "$BASE_URL"
    fi
}
trap cleanup_search_fixture EXIT

log_step "Checking Search Service at $SEARCH_BASE_URL..."
http_request "GET" "/actuator/health" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Search service is healthy"

log_step "Creating a searchable catalog entry..."
http_request "POST" "/software" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    "{\"name\":\"$SEARCH_NAME\",\"description\":\"ReleaseHub search manual test\",\"category\":\"$SEARCH_CATEGORY\",\"tags\":[\"search\",\"manual-test\"]}"
assert_status 201 "Create search fixture"
SOFTWARE_ID=$(json_val "$HTTP_BODY" ".id")

if [ -z "$SOFTWARE_ID" ] || [ "$SOFTWARE_ID" = "null" ]; then
    log_fail "Could not retrieve the search fixture ID"
    suite_summary
    exit 1
fi

log_step "Adding a Linux release to populate the OS facet..."
http_request "POST" "/software/$SOFTWARE_ID/versions" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    '{"versionNumber":"1.0.0","os":"LINUX","releaseNotes":"Search test release","fileRef":"search-test/1.0.0"}'
assert_status 201 "Create Linux release"

log_step "Searching for the exact software name..."
http_request "GET" "/search?q=$SEARCH_QUERY&size=10" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Exact search"
assert_json_eq ".hits[0].name" "$SEARCH_NAME" "Search result contains the software"

log_step "Checking typo-tolerant search..."
http_request "GET" "/search?q=ReleaseHub%20Serch%20Manual%20$SEARCH_TIMESTAMP&size=10" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Fuzzy search"
assert_json_eq ".hits[0].id" "$SOFTWARE_ID" "Fuzzy search finds the fixture"

log_step "Filtering results and checking OS/category facets..."
http_request "GET" "/search?q=$SEARCH_QUERY&os=LINUX&category=$SEARCH_CATEGORY" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Filter by OS and category"
assert_json_eq ".total" "1" "Search filters return one matching result"
assert_json_eq ".facets.os[0].key" "LINUX" "OS facet includes Linux"
assert_json_eq ".facets.category[0].key" "$SEARCH_CATEGORY" "Category facet includes the fixture category"

log_step "Updating the catalog entry and verifying the index update..."
UPDATED_NAME="${SEARCH_NAME}Updated"
UPDATED_QUERY="ReleaseHub%20Search%20Manual%20${SEARCH_TIMESTAMP}Updated"
http_request "PUT" "/software/$SOFTWARE_ID" \
    "$CONTENT_TYPE_JSON;$ROLE_PUBLISHER" \
    "{\"name\":\"$UPDATED_NAME\",\"description\":\"Updated ReleaseHub search manual test\",\"category\":\"$SEARCH_CATEGORY\",\"tags\":[\"search\",\"updated\"]}"
assert_status 200 "Update search fixture"
http_request "GET" "/search?q=$UPDATED_QUERY&size=10" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Search after update"
assert_json_eq ".hits[0].name" "$UPDATED_NAME" "Updated name is indexed"

log_step "Deleting the catalog entry and verifying index removal..."
http_request "DELETE" "/software/$SOFTWARE_ID" "$ROLE_ADMIN" ""
assert_status 204 "Delete search fixture"
SOFTWARE_ID=""
http_request "GET" "/search?q=$UPDATED_QUERY&size=10" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Search after delete"
assert_json_eq ".total" "0" "Deleted software is removed from search"

log_step "Running a full alias-based reindex..."
http_request "POST" "/admin/reindex" "" "" "$SEARCH_BASE_URL"
assert_status 200 "Reindex catalog"
assert_json_eq ".alias" "software" "Reindex response reports the live alias"
PREVIOUS_INDEX=$(json_val "$HTTP_BODY" ".previousIndex")
CURRENT_INDEX=$(json_val "$HTTP_BODY" ".index")
if [ -n "$PREVIOUS_INDEX" ] && [ -n "$CURRENT_INDEX" ] && [ "$PREVIOUS_INDEX" != "$CURRENT_INDEX" ]; then
    log_pass "Reindex switched from $PREVIOUS_INDEX to $CURRENT_INDEX"
else
    log_fail "Reindex did not report a new versioned index"
fi

suite_summary
