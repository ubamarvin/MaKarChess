#!/bin/zsh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="8080"

EXISTING_PID="$(lsof -ti tcp:${PORT} -sTCP:LISTEN 2>/dev/null || true)"

if [[ -n "${EXISTING_PID}" ]]; then
  echo "Stopping existing server on port ${PORT} (PID ${EXISTING_PID})..."
  kill -9 "${EXISTING_PID}" || true
  sleep 1
fi

cd "${ROOT_DIR}"
echo "Starting MaKarChess on http://127.0.0.1:${PORT}/"
exec sbt "runMain makarchess.api.ServerApp"
