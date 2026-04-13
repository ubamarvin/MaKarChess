# Web UI Documentation

This document explains how to launch the standalone web UI and which REST API routes it uses.

## Overview

The web frontend lives in:

```text
web-ui/
```

It is a standalone browser app built with:

- HTML
- CSS
- JavaScript
- Three.js

The frontend talks to the backend REST API at:

```text
http://127.0.0.1:8080
```

## Start the backend

Start the chess REST server first:

```bash
sbt "runMain makarchess.api.ServerApp"
```

The server runs on:

```text
http://127.0.0.1:8080
```

## Start the web UI

Open a second terminal and start a simple static file server inside `web-ui/`:

```bash
python3 -m http.server 4173
```

Run that command in:

```text
/home/marvin/SA/makarchess/web-ui
```

Then open the frontend in your browser:

```text
http://127.0.0.1:4173
```

## First usage flow

1. Start the backend server.
2. Start the static web server in `web-ui/`.
3. Open `http://127.0.0.1:4173` in your browser.
4. Wait for the backend status message.
5. Click `New Game`.
6. Play by either:
   - entering a UCI move such as `e2e4`
   - clicking a source square and then a target square
7. Optionally paste a FEN string and click `Load FEN`.
8. Optionally paste a PGN string and click `Load PGN Replay`.
9. Use `Replay Back` and `Replay Forward` to move through a loaded replay.

## Available UI controls

The web UI includes:

- `New Game`
- `Reset Game`
- `Bot Type` dropdown
- `Bot Plays` dropdown
- `Modeled Side` dropdown
- `UCI Move` input
- `Submit Move`
- `FEN` text area
- `Load FEN`
- `PGN Replay` text area
- `Load PGN Replay`
- `Replay Back`
- `Replay Forward`
- replay status display

## Route usage

The frontend uses the following REST routes.

### `GET /health`

Used on page load to check whether the backend is reachable.

Example:

```text
GET http://127.0.0.1:8080/health
```

### `POST /game/new`

Used when the user clicks `New Game`.

If no dropdown options are selected, the frontend sends no JSON body.

Example without configuration:

```text
POST http://127.0.0.1:8080/game/new
```

Example with configuration:

```json
{
  "botType": "random",
  "botPlays": "black",
  "modeledSide": "white"
}
```

This route returns the initial `GameStateResponse`, which the frontend uses to render pieces on the 3D board.

### `POST /game/reset`

Used when the user clicks `Reset Game`.

Example:

```text
POST http://127.0.0.1:8080/game/reset
```

This resets the game to the initial position and returns a fresh `GameStateResponse`.

### `GET /game/status`

Used after `New Game`, after a successful move, after FEN/PGN load, and after replay stepping.

Example:

```text
GET http://127.0.0.1:8080/game/status
```

The frontend uses it to show:

- side to move
- phase
- winner if present
- check state
- status line
- current player line

### `GET /game/replay`

Used to keep the replay controls and replay status display synchronized with backend replay state.

Example:

```text
GET http://127.0.0.1:8080/game/replay
```

The frontend uses it to show:

- whether replay mode is active
- current replay index
- replay length
- whether replay buttons should be enabled or disabled

### `POST /game/fen`

Used when the user clicks `Load FEN`.

Example request body:

```json
{
  "fen": "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
}
```

Example:

```text
POST http://127.0.0.1:8080/game/fen
```

This route replaces the current board position and returns the updated `GameStateResponse`.

### `POST /game/pgn`

Used when the user clicks `Load PGN Replay`.

Example request body:

```json
{
  "pgn": "1. e4 e5 2. Nf3 Nc6"
}
```

Example:

```text
POST http://127.0.0.1:8080/game/pgn
```

This route loads replay mode at the **start position** and returns the current replay `GameStateResponse`.

### `POST /game/replay/backward`

Used when the user clicks `Replay Back`.

Example:

```text
POST http://127.0.0.1:8080/game/replay/backward
```

The backend returns the updated replay board state one step earlier.

### `POST /game/replay/forward`

Used when the user clicks `Replay Forward`.

Example:

```text
POST http://127.0.0.1:8080/game/replay/forward
```

The backend returns the updated replay board state one step later.

### `POST /game/move`

Used when the user submits a move.

Example request body:

```json
{
  "uci": "e2e4"
}
```

Example:

```text
POST http://127.0.0.1:8080/game/move
```

The backend validates the move and returns the updated `GameStateResponse`.

## Frontend behavior

### New Game

When the user clicks `New Game`:

- the frontend reads the dropdown values
- it builds an optional config object
- it calls `POST /game/new`
- it renders the returned board state
- it fetches `/game/status`
- it updates the status panel

### Reset Game

When the user clicks `Reset Game`:

- the frontend calls `POST /game/reset`
- it rerenders the board
- it refreshes the status panel

### Load FEN

When the user pastes a FEN string and clicks `Load FEN`:

- the frontend sends `POST /game/fen`
- it rerenders the board from the loaded position
- it refreshes `/game/status`
- it refreshes `/game/replay`
- replay controls become inactive unless a replay is later loaded

### Load PGN Replay

When the user pastes a PGN string and clicks `Load PGN Replay`:

- the frontend sends `POST /game/pgn`
- it rerenders the board at replay index `0`
- it refreshes `/game/status`
- it refreshes `/game/replay`
- replay navigation buttons are enabled according to the replay bounds

### Replay navigation

When the user clicks `Replay Back` or `Replay Forward`:

- the frontend sends the matching replay step request
- it rerenders the board from the returned replay state
- it refreshes `/game/status`
- it refreshes `/game/replay`
- button enablement updates automatically at replay boundaries

### Manual UCI input

When the user enters a move in the text field and clicks `Submit Move`:

- the frontend sends `POST /game/move`
- on success it rerenders the board
- it refreshes the status panel
- it refreshes replay status
- on error it shows the backend error message

### Click-to-move

The 3D board also supports click-to-move:

- first click selects a source square
- second click selects a target square
- the frontend builds a UCI move string like `e2e4`
- the frontend sends that move through `POST /game/move`

The frontend does not validate chess legality itself. The backend remains the source of truth.

Normal move submission also clears replay mode on the backend, so replay status becomes inactive after a successful non-replay move.

## Browser/service worker note

If an older localhost web app appears instead of the new UI, this is usually caused by a stale service worker from another project.

The new frontend already tries to unregister old service workers automatically, but if needed you can also:

- hard refresh the page
- use an incognito window
- open the UI on another port such as `4174`
- clear site data for localhost in browser devtools

## CORS note

The backend server has been configured so the standalone frontend can call it from another port on localhost.

## Related files

- `web-ui/index.html`
- `web-ui/style.css`
- `web-ui/js/main.js`
- `web-ui/js/api.js`
- `web-ui/js/scene.js`
- `web-ui/js/board.js`
- `web-ui/js/pieces.js`
- `web-ui/js/interaction.js`
- `web-ui/js/ui.js`
- `Documentation/RestDocs.md`
