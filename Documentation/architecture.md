# Architecture Overview

This document is a practical map of the MaKarChess codebase. It focuses on:

- main entry points
- where runtime state lives
- how requests flow through the backend
- how the web UI talks to the backend
- which files are best to read first

## Big picture

The project currently has three main surfaces:

- CLI / desktop application
- REST backend
- standalone web frontend

All of them are built on the same core chess logic and controller layer.

## Main entry points

### CLI / desktop app

- `src/main/scala/makarchess/Main.scala`

Responsibilities:

- parses startup flags
- creates a `ChessController`
- wires optional bot behavior
- wires optional opponent modeling
- launches TUI or GUI

### REST backend

- `src/main/scala/makarchess/api/ServerApp.scala`

Responsibilities:

- creates `GameRegistry`
- creates `ApiGameService`
- mounts `GameRoutes`
- starts the HTTP server on `127.0.0.1:8080`
- enables CORS for the standalone frontend

### Web frontend

- `web-ui/index.html`
- `web-ui/js/main.js`

Responsibilities:

- provides the browser UI
- sends REST requests to the backend
- updates board rendering and status fields
- supports FEN loading, PGN replay loading, and replay navigation

## Core backend architecture

A REST request usually flows through the backend like this:

```text
HTTP request
-> GameRoutes
-> ApiGameService
-> GameRegistry.currentController
-> ChessController
-> model / ChessRules / parsers / replay code
-> response DTO
```

## Backend layers

### Routing layer

- `src/main/scala/makarchess/api/routes/GameRoutes.scala`

Responsibilities:

- defines all HTTP routes
- decodes incoming JSON
- calls the service layer
- maps service results to HTTP responses

Important routes:

- `GET /health`
- `POST /game/new`
- `POST /game/reset`
- `GET /game/board`
- `GET /game/status`
- `GET /game/replay`
- `POST /game/fen`
- `POST /game/pgn`
- `POST /game/replay/forward`
- `POST /game/replay/backward`
- `POST /game/move`

### Service layer

- `src/main/scala/makarchess/api/service/ApiGameService.scala`

Responsibilities:

- validates API inputs
- calls controller operations via the registry
- translates domain/controller failures into API failures
- builds response DTOs

This is the best file to read if you want to understand the public API behavior.

### Shared runtime state

- `src/main/scala/makarchess/api/service/GameRegistry.scala`

Responsibilities:

- owns the shared backend `ChessController`
- creates fresh configured controllers for new games
- resets the current controller when needed

Important design point:

- the backend exposes one shared in-memory game session

### Controller layer

- `src/main/scala/makarchess/controller/ChessController.scala`

Responsibilities:

- orchestrates chess operations
- owns the current `ChessModel`
- owns active replay state
- clears or activates replay depending on operation
- delegates legality and replay mechanics to the model layer

This is the main application brain of the project.

## Where runtime state lives

### Shared backend session state

For the REST backend, live session state is centered around:

- `GameRegistry`
- `ChessController`

Important mutable controller fields include:

- current model state
- active replay cursor
- opponent-model-related UI state

### Chess state and legality

Core chess rules and board transitions live in `makarchess.model`, especially:

- `ChessRules`
- `ChessState`
- related move and piece types

## FEN / PGN / replay area

This feature cluster spans controller, model, DTOs, and frontend.

### Main files

- `src/main/scala/makarchess/controller/ChessController.scala`
- `src/main/scala/makarchess/model/PgnReplay.scala`
- `src/main/scala/makarchess/model/PgnReplayCursor.scala`
- `src/main/scala/makarchess/api/dto/ImportReplayRequests.scala`

### Responsibilities

#### `ChessController`

Handles:

- `loadFenFromString`
- `loadReplayFromPgnString`
- `stepReplayForward`
- `stepReplayBackward`
- replay activity and replay metadata

#### `PgnReplay`

Handles:

- building a replay from SAN tokens
- resolving SAN against legal moves
- creating a replay cursor over a sequence of board states

#### `PgnReplayCursor`

Handles:

- current replay index
- stepping forward and backward
- jumping to start or end

### Important replay invariant

PGN replay construction should start from the standard initial chess position.

That means replay building is anchored to:

- `ChessRules.initialState`

and not to the current mutable board state.

This prevents repeated PGN loads from failing after a prior replay session.

## Frontend architecture

The frontend flow usually looks like this:

```text
User interaction
-> web-ui/js/main.js
-> web-ui/js/api.js
-> backend REST endpoint
-> JSON response
-> main.js updates frontend state
-> ui.js updates DOM fields and button states
-> board/piece rendering updates the 3D scene
```

## Frontend files by responsibility

### HTML shell

- `web-ui/index.html`

Responsibilities:

- defines controls and layout
- hosts the board container and status panel

### API client

- `web-ui/js/api.js`

Responsibilities:

- centralizes all `fetch` calls to the backend

Key functions include:

- `health`
- `newGame`
- `resetGame`
- `getBoard`
- `getStatus`
- `getReplayStatus`
- `loadFen`
- `loadPgnReplay`
- `replayForward`
- `replayBackward`
- `makeMove`

### Frontend coordinator

- `web-ui/js/main.js`

Responsibilities:

- attaches event listeners
- owns frontend runtime state
- reacts to button clicks and move submissions
- synchronizes board, status, and replay metadata with the backend

### DOM/status bindings

- `web-ui/js/ui.js`

Responsibilities:

- collects DOM references
- reads user input values
- updates messages and status labels
- enables and disables replay buttons based on replay metadata

### Rendering and interaction

- `web-ui/js/scene.js`
- `web-ui/js/board.js`
- `web-ui/js/pieces.js`
- `web-ui/js/interaction.js`

Responsibilities:

- create and maintain the Three.js scene
- render the board
- render the pieces
- implement click-to-move behavior

## Common request flows

### Start a new game

```text
web-ui/js/main.js
-> api.newGame()
-> POST /game/new
-> GameRoutes
-> ApiGameService.newGame
-> GameRegistry.newGame
-> ChessController created/configured
-> response returned
-> frontend rerenders board and status
```

### Submit a move

```text
main.js
-> api.makeMove(uci)
-> POST /game/move
-> ApiGameService.makeMove
-> ChessController.handleMoveInput
-> model / rules execution
-> replay state cleared on normal move
-> updated state returned
```

### Load FEN

```text
main.js
-> api.loadFen(fen)
-> POST /game/fen
-> ApiGameService.loadFen
-> ChessController.loadFenFromString
-> FEN parser
-> ChessState
-> replay cleared
-> updated state returned
```

### Load PGN replay

```text
main.js
-> api.loadPgnReplay(pgn)
-> POST /game/pgn
-> ApiGameService.loadPgnReplay
-> ChessController.loadReplayFromPgnString
-> PgnReplay.buildCursor(initial state, SAN moves)
-> replay cursor stored
-> state returned at replay index 0
```

### Replay navigation

```text
main.js
-> api.replayForward() or api.replayBackward()
-> POST /game/replay/forward or /game/replay/backward
-> ApiGameService replay methods
-> ChessController step methods
-> replay cursor index updated
-> updated state returned
```

## Tests worth reading first

If you want to understand the system by examples, start with these:

- `src/test/scala/ApiRoutesSpec.scala`
- `src/test/scala/ChessControllerSpec.scala`
- `src/test/scala/ParserArchitectureSpec.scala`
- `src/test/scala/BotSpec.scala`

Why these matter:

- `ApiRoutesSpec.scala`
  - shows backend behavior from the HTTP boundary
- `ChessControllerSpec.scala`
  - shows the main orchestration behavior
- `ParserArchitectureSpec.scala`
  - shows how PGN/FEN parsing and replay are expected to behave
- `BotSpec.scala`
  - shows controller interaction with bot logic

## Suggested reading order

If you want to rebuild understanding quickly, read these in order.

### Backend path

1. `src/main/scala/makarchess/api/ServerApp.scala`
2. `src/main/scala/makarchess/api/routes/GameRoutes.scala`
3. `src/main/scala/makarchess/api/service/ApiGameService.scala`
4. `src/main/scala/makarchess/api/service/GameRegistry.scala`
5. `src/main/scala/makarchess/controller/ChessController.scala`

### Replay internals

6. `src/main/scala/makarchess/model/PgnReplay.scala`
7. `src/main/scala/makarchess/model/PgnReplayCursor.scala`

### Frontend path

8. `web-ui/js/api.js`
9. `web-ui/js/main.js`
10. `web-ui/js/ui.js`

### Test path

11. `src/test/scala/ApiRoutesSpec.scala`
12. `src/test/scala/ChessControllerSpec.scala`
13. `src/test/scala/ParserArchitectureSpec.scala`

## Quick navigation guide

Use this when you forget where a concern lives.

- HTTP endpoints
  - `GameRoutes.scala`

- API validation and error mapping
  - `ApiGameService.scala`

- shared backend game instance
  - `GameRegistry.scala`

- main application orchestration
  - `ChessController.scala`

- SAN replay construction
  - `PgnReplay.scala`

- replay cursor/index logic
  - `PgnReplayCursor.scala`

- frontend backend calls
  - `web-ui/js/api.js`

- frontend button behavior
  - `web-ui/js/main.js`

- frontend DOM/status rendering
  - `web-ui/js/ui.js`

## Compact mental model

A simple way to think about the system is:

- `ChessController` is the main application brain
- `GameRegistry` owns the shared backend session
- `ApiGameService` translates controller behavior into API behavior
- `GameRoutes` exposes that behavior over HTTP
- `PgnReplay` computes replay state history
- `web-ui/js/main.js` is the browser-side coordinator

If you revisit the code later, start from those six points.
