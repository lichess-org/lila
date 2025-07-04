#!/usr/bin/env bash

set -e

cd "$(dirname "${BASH_SOURCE:-$0}")/.build"

MIN_NODE="$(cat ../../.node-version)"
CUR_NODE=$(node -v 2>/dev/null || echo "v0.0.0")
LOWEST="$(printf "%s\n%s" "$MIN_NODE" "$CUR_NODE" | sort -V | head -n 1)"
if [ "$LOWEST" != "$MIN_NODE" ]; then
  echo "Nodejs $MIN_NODE or later is required."
  exit 1
fi

if ! yes | pnpm install --ignore-workspace >/dev/null 2>&1; then
  pnpm install --loglevel debug --ignore-workspace
  exit $?
fi

node --experimental-strip-types --no-warnings src/main.ts "$@"
