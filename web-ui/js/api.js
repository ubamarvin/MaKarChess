const API_BASE = "http://127.0.0.1:8080";
let authTokenProvider = null;

class ApiClientError extends Error {
  constructor(message, status = 0, details = null) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.details = details;
  }
}

async function parseJsonSafely(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    throw new ApiClientError("Malformed response from backend.", response.status);
  }
}

async function request(path, options = {}) {
  const url = `${API_BASE}${path}`;
  const token = authTokenProvider ? await authTokenProvider() : null;
  const headers = new Headers(options.headers || {});
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    });
    const payload = await parseJsonSafely(response);

    if (!response.ok) {
      const message = payload?.message || `Backend request failed with status ${response.status}.`;
      throw new ApiClientError(message, response.status, payload);
    }

    return payload;
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }

    throw new ApiClientError("Backend not reachable. Make sure the REST server is running.");
  }
}

function buildJsonOptions(method, body) {
  if (body == null) {
    return { method };
  }

  return {
    method,
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  };
}

export function setAuthTokenProvider(provider) {
  authTokenProvider = typeof provider === "function" ? provider : null;
}

export { API_BASE, ApiClientError };

export function health() {
  return request("/health");
}

export function newGame(config = null) {
  return request("/game/new", buildJsonOptions("POST", config));
}

export function resetGame() {
  return request("/game/reset", buildJsonOptions("POST"));
}

export function getBoard() {
  return request("/game/board");
}

export function getStatus() {
  return request("/game/status");
}

export function getReplayStatus() {
  return request("/game/replay");
}

export function loadFen(fen) {
  return request("/game/fen", buildJsonOptions("POST", { fen }));
}

export function loadPgnReplay(pgn) {
  return request("/game/pgn", buildJsonOptions("POST", { pgn }));
}

export function replayForward() {
  return request("/game/replay/forward", buildJsonOptions("POST"));
}

export function replayBackward() {
  return request("/game/replay/backward", buildJsonOptions("POST"));
}

export function makeMove(uci) {
  return request("/game/move", buildJsonOptions("POST", { uci }));
}
