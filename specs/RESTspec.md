# REST API plan for web chess UI

This plan adds a minimal http4s + Ember REST adapter around the existing controller so a web UI can play a single shared chess game using structured JSON board state.

## Scope
- Build a separate API adapter layer without changing chess domain rules.
- Support one in-memory shared game on `localhost:8080`.
- Expose only the operations needed for play: create/reset game, fetch state, fetch status, make move, health check.
- Return structured board data only, not board text, for the first web-facing version.
- Use dedicated DTOs for request/response payloads rather than exposing raw domain types directly.

## Existing surfaces to reuse
- `ChessController.startNewGame()` / `restartGame()` for game lifecycle.
- `ChessController.handleMoveInput(input)` for UCI move execution.
- `ChessController.model.chessState` as the authoritative board/state source.
- `ChessController.snapshot` only for status-derived fields if useful.
- `UpickleGameStateJsonCodec` as a reference for current JSON conventions for `Color`, `PieceType`, `Position`, `Piece`, and `GamePhase`.

## Proposed implementation shape
- Add API package(s) under the existing project first, even if named as a separate adapter area, to keep the implementation minimal:
  - `makarchess.api.ServerApp`
  - `makarchess.api.routes.GameRoutes`
  - `makarchess.api.dto.*`
  - `makarchess.api.json.JsonCodecs`
  - `makarchess.api.service.ApiGameService`
  - `makarchess.api.service.GameRegistry`
- Add `http4s`, `http4s-ember-server`, `http4s-dsl`, `http4s-circe` or `http4s` + `upickle` integration, and `cats-effect` dependencies in `build.sbt`.
- Keep the current desktop entrypoint untouched; add a dedicated server entrypoint for the API.

## Endpoint plan
- `GET /health`
  - returns `{ "status": "ok" }`
- `POST /game/new`
  - creates or reinitializes the single in-memory game
  - returns full `GameStateResponse`
- `POST /game/reset`
  - resets the current game to the initial position
  - returns full `GameStateResponse`
- `GET /game/board`
  - returns full `GameStateResponse`
- `GET /game/status`
  - returns a smaller `GameStatusResponse` derived from the same underlying state
- `POST /game/move`
  - accepts `MoveRequest { uci }`
  - on success returns updated `GameStateResponse`
  - maps invalid input to `400`, illegal/game-over to `409`

## Board transfer format
Use a machine-friendly DTO rather than exposing `ChessState` directly. This DTO approach is confirmed.

Suggested response shape:
- `board`: array of occupied squares
  - each item: `{ "position": { "file": "e", "rank": 4 }, "piece": { "color": "White", "kind": "Pawn" } }`
- `sideToMove`: `White | Black`
- `phase`: tagged game phase object, aligned with the current JSON codec style
- `castling`: current castling rights
- `enPassant`: nullable position
- `halfmoveClock`
- `fullmoveNumber`
- optional status helpers for the web UI:
  - `isCheck`
  - `legalMoveCount` or defer until needed

Rationale:
- This shape is stable and easy for a browser to render into an 8x8 board.
- It avoids leaking replay/persistence concerns.
- It stays close to current model types and current JSON conventions.

## Service/controller mapping
- `GameRegistry`
  - owns the one active `ChessController`
  - initializes it from `ChessModel()`
  - returns the active controller to the service layer
- `ApiGameService`
  - calls controller methods
  - converts `ChessState` and `GameSnapshot` into API DTOs
  - centralizes error translation from `MoveAttemptError` / parse failures into API errors
- `GameRoutes`
  - keeps HTTP concerns only: decode request, call service, encode response, choose status code

## Error mapping
- `400 Bad Request`
  - malformed JSON
  - missing/blank `uci`
  - invalid move syntax
- `409 Conflict`
  - illegal move
  - game already over
- `500 Internal Server Error`
  - unexpected failures in server wiring/serialization

## Testing plan
- Add route/service tests for:
  - `GET /health`
  - `POST /game/new`
  - `GET /game/board`
  - `POST /game/move` with legal move
  - `POST /game/move` with invalid syntax
  - `POST /game/move` with illegal move
  - `POST /game/reset`
- Keep tests focused on API contract and status mapping, not chess rules already covered elsewhere.

## Open implementation choices
- Prefer DTOs over returning raw `ChessState` JSON directly.
- Prefer one shared controller instance now, but keep `GameRegistry` shaped so session/game IDs can be added later.
- Prefer preserving existing desktop MVC code untouched; the REST layer should be additive.
