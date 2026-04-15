#!/bin/zsh

set -euo pipefail

PORT="8080"
EXISTING_PID="$(lsof -ti tcp:${PORT} -sTCP:LISTEN 2>/dev/null || true)"

if [[ -z "${EXISTING_PID}" ]]; then
  echo "No server running on port ${PORT}."
  exit 0
fi

echo "Stopping server on port ${PORT} (PID ${EXISTING_PID})..."
kill -9 "${EXISTING_PID}"
echo "Server stopped."
