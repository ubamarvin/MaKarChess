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

## Available UI controls

The web UI includes:

- `New Game`
- `Reset Game`
- `Refresh Board`
- `Bot Type` dropdown
- `Bot Plays` dropdown
- `Modeled Side` dropdown
- `UCI Move` input
- `Submit Move`

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

### `GET /game/board`

Used when the user clicks `Refresh Board`.

Example:

```text
GET http://127.0.0.1:8080/game/board
```

The frontend uses the returned board state to rerender the pieces.

### `GET /game/status`

Used after `New Game`, after a successful move, and on `Refresh Board`.

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

### Refresh Board

When the user clicks `Refresh Board`:

- the frontend calls `GET /game/board`
- the frontend calls `GET /game/status`
- it rerenders the board and status panel from current backend state

### Manual UCI input

When the user enters a move in the text field and clicks `Submit Move`:

- the frontend sends `POST /game/move`
- on success it rerenders the board
- it refreshes the status panel
- on error it shows the backend error message

### Click-to-move

The 3D board also supports click-to-move:

- first click selects a source square
- second click selects a target square
- the frontend builds a UCI move string like `e2e4`
- the frontend sends that move through `POST /game/move`

The frontend does not validate chess legality itself. The backend remains the source of truth.

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
