#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEY_DIR="$SCRIPT_DIR/keys"

mkdir -p "$KEY_DIR"
if [ -e "$KEY_DIR/private.pem" ] || [ -e "$KEY_DIR/public.pem" ]; then
    echo "Refusing to overwrite an existing JWT key. Remove the key pair explicitly to rotate it." >&2
    exit 1
fi

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEY_DIR/private.pem"
openssl pkey -in "$KEY_DIR/private.pem" -pubout -out "$KEY_DIR/public.pem"
chmod 600 "$KEY_DIR/private.pem"
chmod 644 "$KEY_DIR/public.pem"
echo "Created local RS256 keys under gateway/keys. The private key is git-ignored."
