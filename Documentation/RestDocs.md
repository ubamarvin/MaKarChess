# REST API Documentation

This document describes the implemented REST API for playing a single shared chess game over HTTP.

## Overview

- **Server entrypoint**: `makarchess.api.ServerApp`
- **Bind address**: `127.0.0.1:8080`
- **Content type**: JSON
- **Game model**: one shared in-memory chess game

## Running the server

Start the API server with:

```bash
sbt "runMain makarchess.api.ServerApp"
```

Once started, the API is available at:

```text
http://127.0.0.1:8080
```

## API behavior

- The server keeps **one shared game instance** in memory.
- `POST /game/new` creates a fresh controller/game instance.
- `POST /game/reset` resets the current game back to the initial position.
- `POST /game/move` accepts a move in **UCI notation** such as `e2e4`.
- Responses return **structured board state**, not rendered board text.

## Data formats

### `GameConfigRequest`

Optional request body for `POST /game/new`.

```json
{
  "botType": "random",
  "botPlays": "black",
  "modeledSide": "white"
}
```

Fields:

- `botType`
  - optional
  - defaults to `random` when omitted or blank
  - supported values: `random`, `greedy`, `defensive`, `aggressive`
- `botPlays`
  - optional
  - supported values: `white`, `black`
- `modeledSide`
  - optional
  - supported values: `white`, `black`

### `MoveRequest`

Used by `POST /game/move`.

```json
{
  "uci": "e2e4"
}
```

### `ErrorResponse`

Used for `400`, `409`, and possible `500` responses.

```json
{
  "message": "Illegal move."
}
```

### `GameStateResponse`

Returned by:

- `POST /game/new`
- `POST /game/reset`
- `GET /game/board`
- `POST /game/move`

Shape:

```json
{
  "board": [
    {
      "position": {
        "file": "a",
        "rank": 1
      },
      "piece": {
        "color": "White",
        "kind": "Rook"
      }
    }
  ],
  "sideToMove": "White",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "castling": {
    "whiteKingside": true,
    "whiteQueenside": true,
    "blackKingside": true,
    "blackQueenside": true
  },
  "enPassant": null,
  "halfmoveClock": 0,
  "fullmoveNumber": 1,
  "isCheck": false
}
```

### `GameStatusResponse`

Returned by `GET /game/status`.

```json
{
  "sideToMove": "White",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "statusLine": "",
  "currentPlayerLine": "White to move",
  "isCheck": false
}
```

## Endpoints

## `GET /health`

Simple health check.

### Example

```bash
curl http://127.0.0.1:8080/health
```

### Success response

```json
{
  "status": "ok"
}
```

### Status codes

- `200 OK`

## `POST /game/new`

Creates a new shared chess game and returns the initial state.

This route may be called either:

- without a request body, for a default game
- with an optional `GameConfigRequest` JSON body to configure bots and opponent modeling

### Example

```bash
curl -X POST http://127.0.0.1:8080/game/new
```

### Example with configuration

```bash
curl -X POST http://127.0.0.1:8080/game/new \
  -H "Content-Type: application/json" \
  -d '{"botType":"random","botPlays":"black","modeledSide":"white"}'
```

### Optional request body

```json
{
  "botType": "random",
  "botPlays": "black",
  "modeledSide": "white"
}
```

### Option behavior

- `botType` selects which bot implementation is attached when `botPlays` is set.
- `botPlays` chooses which color the bot controls.
- `modeledSide` chooses which side is observed by the opponent-modeling system.
- if `botPlays` is omitted, no bot is attached.
- if `modeledSide` is omitted, opponent modeling is not enabled.
- if `botType` is omitted, the API uses `random`.

### Success response

```json
{
  "board": [
    {
      "position": {
        "file": "a",
        "rank": 1
      },
      "piece": {
        "color": "White",
        "kind": "Rook"
      }
    }
  ],
  "sideToMove": "White",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "castling": {
    "whiteKingside": true,
    "whiteQueenside": true,
    "blackKingside": true,
    "blackQueenside": true
  },
  "enPassant": null,
  "halfmoveClock": 0,
  "fullmoveNumber": 1,
  "isCheck": false
}
```

### Status codes

- `200 OK`

## `POST /game/reset`

Resets the current shared game to the default starting position.

### Example

```bash
curl -X POST http://127.0.0.1:8080/game/reset
```

### Success response

Returns the same structure as `GameStateResponse`.

### Status codes

- `200 OK`

## `GET /game/board`

Returns the current board state as structured JSON.

### Example

```bash
curl http://127.0.0.1:8080/game/board
```

### Success response

```json
{
  "board": [
    {
      "position": {
        "file": "e",
        "rank": 4
      },
      "piece": {
        "color": "White",
        "kind": "Pawn"
      }
    }
  ],
  "sideToMove": "Black",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "castling": {
    "whiteKingside": true,
    "whiteQueenside": true,
    "blackKingside": true,
    "blackQueenside": true
  },
  "enPassant": {
    "file": "e",
    "rank": 3
  },
  "halfmoveClock": 0,
  "fullmoveNumber": 1,
  "isCheck": false
}
```

### Status codes

- `200 OK`

## `GET /game/status`

Returns a compact status view of the current game.

### Example

```bash
curl http://127.0.0.1:8080/game/status
```

### Success response

```json
{
  "sideToMove": "White",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "statusLine": "",
  "currentPlayerLine": "White to move",
  "isCheck": false
}
```

### Notes

- `statusLine` contains special game-state messages such as checkmate or draw.
- `currentPlayerLine` mirrors the current player prompt used by the desktop application.
- `isCheck` is `true` only while the game is still in progress and the side to move is in check.

### Status codes

- `200 OK`

## `POST /game/move`

Applies a move to the current shared game.

### Request body

```json
{
  "uci": "e2e4"
}
```

### Example

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":"e2e4"}'
```

### Success response

Returns the updated `GameStateResponse`.

Example after `e2e4`:

```json
{
  "board": [
    {
      "position": {
        "file": "e",
        "rank": 4
      },
      "piece": {
        "color": "White",
        "kind": "Pawn"
      }
    }
  ],
  "sideToMove": "Black",
  "phase": {
    "tag": "InProgress",
    "winner": null
  },
  "castling": {
    "whiteKingside": true,
    "whiteQueenside": true,
    "blackKingside": true,
    "blackQueenside": true
  },
  "enPassant": {
    "file": "e",
    "rank": 3
  },
  "halfmoveClock": 0,
  "fullmoveNumber": 1,
  "isCheck": false
}
```

### Invalid JSON example

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":}'
```

Response:

```json
{
  "message": "Malformed JSON request."
}
```

### Invalid move format example

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":"bad"}'
```

Response:

```json
{
  "message": "Invalid move format. Use e2e4 or e7e8q."
}
```

### Illegal move example

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":"e2e5"}'
```

Response:

```json
{
  "message": "Illegal move."
}
```

### Empty move example

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":""}'
```

Response:

```json
{
  "message": "Move input is required."
}
```

### Status codes

- `200 OK`
- `400 Bad Request`
- `409 Conflict`

## HTTP status code summary

- `200 OK`
  - successful health check
  - successful game creation/reset
  - successful board/status fetch
  - successful move

- `400 Bad Request`
  - malformed JSON for `POST /game/new`
  - invalid `botPlays` or `modeledSide` values
  - unknown bot type when creating a configured game
  - malformed JSON
  - missing or blank `uci`
  - invalid move syntax

- `409 Conflict`
  - illegal move
  - move attempted after game is already over

- `500 Internal Server Error`
  - unexpected server-side failure

## Quick usage flow

Start a new game:

```bash
curl -X POST http://127.0.0.1:8080/game/new
```

Start a configured game with a black random bot and white opponent modeling:

```bash
curl -X POST http://127.0.0.1:8080/game/new \
  -H "Content-Type: application/json" \
  -d '{"botType":"random","botPlays":"black","modeledSide":"white"}'
```

Check the board:

```bash
curl http://127.0.0.1:8080/game/board
```

Play white's first move:

```bash
curl -X POST http://127.0.0.1:8080/game/move \
  -H "Content-Type: application/json" \
  -d '{"uci":"e2e4"}'
```

Read status:

```bash
curl http://127.0.0.1:8080/game/status
```

Reset the game:

```bash
curl -X POST http://127.0.0.1:8080/game/reset
```
