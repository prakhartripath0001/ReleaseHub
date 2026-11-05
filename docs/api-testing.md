# ReleaseHub API Testing Guide

This document provides manual `curl` examples for the Catalog, search, feedback, analytics, versions, and downloads APIs. The executable suites under `api-tests/` cover the same flows.

---

## Prerequisites & Setup

Start the full local stack from the repository root:

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

Wait until the four Spring services report healthy, then run all API suites:

```bash
./api-tests/run-tests.sh all
```

The test runner targets the direct service ports (`8083`–`8086`). Manual gateway requests use Kong on `http://localhost:8000` and require a configured JWT; see the root README for local key setup.

For individual `curl` examples in this guide, the catalog base URL is:

```bash
BASE_URL="http://localhost:8083"
```

---

## 1. Software CRUD APIs

### 1.1 Create Software
Creates a new software catalog entry.

```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{
    "name": "Visual Studio Code",
    "description": "A source code editor",
    "category": "tools",
    "tags": ["editor", "developer-tools"]
  }'
```

- **Expected Status:** `201 Created`
- **Expected Response Body Structure:**
  ```json
  {
    "id": "7a53f798-972c-45e9-abe3-a4bb1893069d",
    "name": "Visual Studio Code",
    "description": "A source code editor",
    "category": "tools",
    "tags": ["editor", "developer-tools"],
    "publisherId": "pub-8565659d",
    "createdAt": "2026-11-04T16:28:20.852292"
  }
  ```

Copy the returned `id` and save it to an environment variable:
```bash
SOFTWARE_ID="7a53f798-972c-45e9-abe3-a4bb1893069d"
```

---

### 1.2 Get All Software
Retrieves paginated list of all software entries.

```bash
curl -i "$BASE_URL/software"
```

- **Expected Status:** `200 OK`
- **Expected Response Body Structure:**
  ```json
  {
    "content": [
      {
        "id": "7a53f798-972c-45e9-abe3-a4bb1893069d",
        "name": "Visual Studio Code",
        "description": "A source code editor",
        "category": "tools",
        "tags": ["editor", "developer-tools"],
        "publisherId": "pub-8565659d",
        "createdAt": "2026-11-04T16:28:21"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
  ```

---

### 1.3 Get Software by ID
Retrieves a single software entry using its unique identifier.

```bash
curl -i "$BASE_URL/software/$SOFTWARE_ID"
```

- **Expected Status:** `200 OK`
- **Expected Response:** Matches the created software object.

---

### 1.4 Update Software
Updates specific metadata of an existing software entry.

```bash
curl -i -X PUT "$BASE_URL/software/$SOFTWARE_ID" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{
    "name": "Visual Studio Code Updated",
    "description": "Updated source code editor",
    "category": "tools",
    "tags": ["editor", "developer-tools", "coding"]
  }'
```

- **Expected Status:** `200 OK`
- **Expected Response:** Updated fields reflected in the returned JSON object.

---

### 1.5 Delete Software
Deletes a software entry.

> **Important:** Run this deletion test only after completing the Version and Download tests below.

```bash
curl -i -X DELETE "$BASE_URL/software/$SOFTWARE_ID" \
  -H "X-USER-ROLE: PUBLISHER"
```

- **Expected Status:** `204 No Content`

---

## 2. Filtering and Pagination

### 2.1 Pagination Parameters
```bash
curl -i "$BASE_URL/software?page=0&size=5"
```
- **Expected Status:** `200 OK` (returns at most 5 items in `content`, with `size: 5`).

### 2.2 Category Filtering
```bash
curl -i "$BASE_URL/software?category=tools"
```
- **Expected Status:** `200 OK` (returns items matching category `tools`).

### 2.3 OS Filtering
```bash
curl -i "$BASE_URL/software?os=LINUX"
```
- **Expected Status:** `200 OK`

### 2.4 Combined Filtering & Pagination
```bash
curl -i "$BASE_URL/software?os=LINUX&category=tools&page=0&size=10"
```
- **Expected Status:** `200 OK`

### 2.5 Page Offset
```bash
curl -i "$BASE_URL/software?page=1&size=10"
```
- **Expected Status:** `200 OK` (empty content if fewer than 10 total items exist).

---

## 3. Version APIs

### 3.1 Create Version 1.0.0
Creates an initial release for the software.

```bash
curl -i -X POST "$BASE_URL/software/$SOFTWARE_ID/versions" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{
    "versionNumber": "1.0.0",
    "os": "LINUX",
    "releaseNotes": "Initial stable release",
    "fileRef": "storage/vscode/1.0.0"
  }'
```

- **Expected Status:** `201 Created`
- **Expected Response Body Structure:**
  ```json
  {
    "id": "18c5e631-0df8-4e58-9a28-6e5410113c4a",
    "softwareId": "7a53f798-972c-45e9-abe3-a4bb1893069d",
    "versionNumber": "1.0.0",
    "os": "LINUX",
    "releaseNotes": "Initial stable release",
    "fileRef": "storage/vscode/1.0.0",
    "releasedAt": "2026-11-04T16:30:15"
  }
  ```

Save the version ID for subsequent tests:
```bash
VERSION_ID="18c5e631-0df8-4e58-9a28-6e5410113c4a"
```

---

### 3.2 Create Version 2.0.0 (Testing Sort Order)
Creates a second release to verify that version queries return releases sorted newest-first.

```bash
curl -i -X POST "$BASE_URL/software/$SOFTWARE_ID/versions" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{
    "versionNumber": "2.0.0",
    "os": "LINUX",
    "releaseNotes": "Second stable release",
    "fileRef": "storage/vscode/2.0.0"
  }'
```

- **Expected Status:** `201 Created`

---

### 3.3 List Versions for a Software
Retrieves all versions associated with a software product, ordered newest-first.

```bash
curl -i "$BASE_URL/software/$SOFTWARE_ID/versions"
```

- **Expected Status:** `200 OK`
- **Verification:** Index `0` should be `2.0.0` and index `1` should be `1.0.0`.

---

### 3.4 Get Specific Version by ID
```bash
curl -i "$BASE_URL/versions/$VERSION_ID"
```

- **Expected Status:** `200 OK`
- **Expected Response:** JSON representation of the version matching `$VERSION_ID`.

---

## 4. Download API

### 4.1 Record Download & Resolve URL
Simulates an end-user downloading a specific software version.

```bash
curl -i "$BASE_URL/download/$VERSION_ID" \
  -H "X-User-Id: usr-48291"
```

- **Expected Status:** `200 OK`
- **Expected Response Body Structure:**
  ```json
  {
    "downloadUrl": "storage/vscode/1.0.0",
    "versionId": "18c5e631-0df8-4e58-9a28-6e5410113c4a",
    "message": "Download recorded successfully"
  }
  ```

---

### 4.2 Verify Download Event in MySQL
Log into your MySQL container to confirm the event was persisted:

```bash
docker exec -it ReleaseHub mysql -uroot -prelease@123 releasehub -e "SELECT id, version_id, user_id, ts FROM download_event ORDER BY ts DESC LIMIT 5;"
```

- **Verification:** An entry with `version_id = $VERSION_ID` and `user_id = usr-48291` should be returned.

---

## 5. Error-Handling Tests

### 5.1 Nonexistent Software ID
Query a non-existent UUID.

```bash
curl -i "$BASE_URL/software/00000000-0000-0000-0000-000000000001"
```

- **Expected Status:** `404 Not Found`
- **Expected Response:**
  ```json
  {
    "status": 404,
    "error": "Not Found",
    "message": "Software not found: 00000000-0000-0000-0000-000000000001"
  }
  ```

---

### 5.2 Empty Software Name (Validation Constraint)
Submit a payload with an empty string for mandatory `name` field.

```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{
    "name": "",
    "description": "Test",
    "category": "tools",
    "tags": []
  }'
```

- **Expected Status:** `400 Bad Request`
- **Expected Response:** Contains `fieldErrors` map with `"name": "Name is required"`.

---

### 5.3 Malformed JSON Payload
Submit syntactically invalid JSON.

```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{"name":'
```

- **Expected Status:** `400 Bad Request` or `500 Internal Server Error` handled by global exception advice.

---

## 6. Authorization Tests

### 6.1 Role: PUBLISHER (Allowed)
```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: PUBLISHER" \
  -d '{"name": "Publisher App", "category": "tools", "tags": []}'
```
- **Expected Status:** `201 Created`

---

### 6.2 Role: ADMIN (Allowed)
```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: ADMIN" \
  -d '{"name": "Admin App", "category": "tools", "tags": []}'
```
- **Expected Status:** `201 Created`

---

### 6.3 Role: USER (Forbidden)
```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -H "X-USER-ROLE: USER" \
  -d '{"name": "Unauthorized App", "category": "tools", "tags": []}'
```
- **Expected Status:** `403 Forbidden`
- **Expected Response:** `{"status":403,"error":"Forbidden","message":"Insufficient role"}`

---

### 6.4 Missing Authorization Header (Forbidden)
```bash
curl -i -X POST "$BASE_URL/software" \
  -H "Content-Type: application/json" \
  -d '{"name": "Anonymous App", "category": "tools", "tags": []}'
```
- **Expected Status:** `403 Forbidden`
- **Expected Response:** `{"status":403,"error":"Forbidden","message":"Insufficient role"}`

 