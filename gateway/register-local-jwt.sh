#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBLIC_KEY="$SCRIPT_DIR/keys/public.pem"
OUTPUT_CONFIG="$SCRIPT_DIR/keys/kong.local.yml"
CONSUMER="${KONG_JWT_CONSUMER:-releasehub-local}"
ISSUER="${KONG_JWT_ISSUER:-releasehub-local}"

if [ ! -r "$PUBLIC_KEY" ]; then
    echo "Missing $PUBLIC_KEY. Run ./gateway/generate-local-jwt-key.sh first." >&2
    exit 1
fi

python3 - "$SCRIPT_DIR/kong.yml" "$PUBLIC_KEY" "$OUTPUT_CONFIG" "$CONSUMER" "$ISSUER" <<'PY'
import pathlib
import re
import sys

source_path, key_path, output_path, consumer, issuer = sys.argv[1:]
config = pathlib.Path(source_path).read_text()
public_key = pathlib.Path(key_path).read_text().rstrip().splitlines()
credential = [
    "consumers:",
    f"  - username: {consumer}",
    "    jwt_secrets:",
    f"      - key: {issuer}",
    "        algorithm: RS256",
    "        rsa_public_key: |",
    *[f"          {line}" for line in public_key],
    "",
]
updated, count = re.subn(r"(?m)^plugins:\s*$", "\n".join(credential) + "plugins:", config, count=1)
if count != 1:
    raise SystemExit("Could not locate the plugins section in gateway/kong.yml")
pathlib.Path(output_path).write_text(updated)
PY
chmod 600 "$OUTPUT_CONFIG"
printf 'Generated DB-less Kong config for JWT consumer "%s" (issuer "%s").\n' "$CONSUMER" "$ISSUER"
printf 'Start/restart Kong with KONG_DECLARATIVE_CONFIG=/kong/keys/kong.local.yml.\n'
