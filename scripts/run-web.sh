#!/bin/zsh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="8080"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  DETECTED_JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 17 2>/dev/null || true)"
  if [[ -n "${DETECTED_JAVA_HOME}" ]]; then
    JAVA_HOME="${DETECTED_JAVA_HOME}"
  fi
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

JAVA_VERSION="$(java -version 2>&1 | head -n 1)"
echo "Using ${JAVA_VERSION}"

EXISTING_PID="$(lsof -ti tcp:${PORT} -sTCP:LISTEN 2>/dev/null || true)"

if [[ -n "${EXISTING_PID}" ]]; then
  echo "Stopping existing server on port ${PORT} (PID ${EXISTING_PID})..."
  kill -9 "${EXISTING_PID}" || true
  sleep 1
fi

cd "${ROOT_DIR}"
echo "Starting MaKarChess on http://127.0.0.1:${PORT}/"
exec sbt "runMain makarchess.api.ServerApp"
