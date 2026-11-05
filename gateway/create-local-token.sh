#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRIVATE_KEY="$SCRIPT_DIR/keys/private.pem"
ISSUER="${KONG_JWT_ISSUER:-releasehub-local}"
EXPIRY_SECONDS="${JWT_EXPIRY_SECONDS:-3600}"

if [ ! -r "$PRIVATE_KEY" ]; then
    echo "Missing $PRIVATE_KEY. Run ./gateway/generate-local-jwt-key.sh first." >&2
    exit 1
fi

python3 - "$PRIVATE_KEY" "$ISSUER" "$EXPIRY_SECONDS" <<'PY'
import base64
import json
import subprocess
import sys
import tempfile
import time

key_path, issuer, expiry_seconds = sys.argv[1:]
encode = lambda data: base64.urlsafe_b64encode(data).rstrip(b"=")
header = encode(json.dumps({"alg": "RS256", "typ": "JWT"}, separators=(",", ":")).encode())
payload = encode(json.dumps(
    {"iss": issuer, "exp": int(time.time()) + int(expiry_seconds)},
    separators=(",", ":"),
).encode())
signing_input = header + b"." + payload
with tempfile.NamedTemporaryFile() as data, tempfile.NamedTemporaryFile() as signature:
    data.write(signing_input)
    data.flush()
    subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", key_path, "-out", signature.name, data.name],
        check=True,
    )
    signature.seek(0)
    print((signing_input + b"." + encode(signature.read())).decode())
PY
