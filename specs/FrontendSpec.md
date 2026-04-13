Build a minimal web frontend for the existing chess REST API using vanilla HTML, CSS, and JavaScript, with Three.js for rendering a simple 3D chessboard.

Goal
- The frontend must allow a user to fully use the existing REST API from a browser.
- The user starts the backend server separately on http://127.0.0.1:8080.
- The frontend is a separate module/app that runs in the browser and communicates with the API using fetch().
- The board must be rendered in 3D with Three.js.
- Chess pieces may initially be simple placeholder meshes such as cylinders, boxes, and cones.
- The first interaction flow must work with one shared game, matching the current API behavior.

Important API facts to integrate
- The API is running at http://127.0.0.1:8080.
- The game is one shared in-memory game.
- Available routes:
  - GET /health
  - POST /game/new
  - POST /game/reset
  - GET /game/board
  - GET /game/status
  - POST /game/move
- POST /game/new may optionally accept:
  - botType
  - botPlays
  - modeledSide
- POST /game/move accepts:
  - { "uci": "e2e4" }
- Board state is returned as structured JSON with occupied squares, sideToMove, phase, castling, enPassant, clocks, and isCheck.

Frontend technology requirements
- Use plain HTML, CSS, and JavaScript only.
- Use Three.js for the 3D scene. Three.js is a JavaScript 3D library; the current site highlights docs/examples and current release information. :contentReference[oaicite:0]{index=0}
- Use OrbitControls so the user can rotate/zoom the camera around the board. OrbitControls is the official Three.js control for orbiting, zooming, and panning around a target. :contentReference[oaicite:1]{index=1}
- Use the browser Fetch API for all HTTP requests. Fetch is the modern promise-based browser API for HTTP requests and is integrated with CORS behavior. :contentReference[oaicite:2]{index=2}

Project structure
Create a separate frontend module, for example:

web-ui/
  index.html
  style.css
  js/
    main.js
    api.js
    scene.js
    board.js
    pieces.js
    interaction.js
    ui.js

File responsibilities

1. index.html
- Main page shell
- Left or top control panel
- Right or center 3D board container
- Status display area
- Move input area
- Buttons:
  - New Game
  - Reset Game
  - Refresh Board
- Dropdowns:
  - botType
  - botPlays
  - modeledSide
- Optional text field for direct UCI input
- Include Three.js as an ES module or module script setup

2. style.css
- Clean two-panel layout:
  - control/status panel
  - board canvas panel
- Dark or neutral background
- Responsive enough to work on desktop
- Keep styling minimal but neat
- Selected square and hover information should be visually obvious in the side panel and in 3D if implemented

3. js/api.js
- Contains all fetch calls to the backend
- Centralize base URL:
  - const API_BASE = "http://127.0.0.1:8080"
- Implement functions:
  - health()
  - newGame(config)
  - resetGame()
  - getBoard()
  - getStatus()
  - makeMove(uci)
- Each function:
  - calls fetch()
  - checks response.ok / status
  - parses JSON
  - throws or returns structured errors
- Centralize error mapping so UI can show backend messages such as:
  - "Illegal move."
  - "Invalid move format. Use e2e4 or e7e8q."

4. js/scene.js
- Creates the Three.js scene
- Creates:
  - scene
  - perspective camera
  - renderer
  - lighting
  - OrbitControls
- Mount renderer into the board container element
- Set a camera angle suitable for viewing the whole chessboard
- Handle resize events
- Export scene setup objects/functions used by board/pieces modules

5. js/board.js
- Responsible only for the board geometry
- Create an 8x8 chessboard in 3D
- Each square should be an individual mesh so it can later be clicked/identified
- Store metadata on each square mesh:
  - file
  - rank
  - algebraic coordinate like "e4"
- Use simple alternating light/dark materials
- Add optional coordinate labels later if desired, but not required now
- Export:
  - createBoard()
  - getSquareMeshAt(file, rank)
  - highlightSquare(file, rank)
  - clearHighlights()

Board coordinate convention
- White perspective at the start:
  - rank 1 near the player
  - rank 8 farther away
  - file a on the left from White’s perspective
- Ensure API positions map consistently to rendered squares

6. js/pieces.js
- Responsible for chess piece placeholder meshes
- No external 3D models yet
- Use simple placeholders:
  - Pawn -> cylinder
  - Rook -> box
  - Knight -> cone
  - Bishop -> cylinder + cone or taller cylinder
  - Queen -> taller cylinder/cone combo
  - King -> tallest placeholder
- White and Black must be visually distinguishable by material/color
- Keep one pieces group in the scene
- Implement:
  - clearPieces()
  - renderPieces(boardState)
- `boardState.board` from API contains only occupied squares, so create meshes only for occupied squares
- Attach metadata to piece meshes:
  - piece color
  - piece kind
  - position
- Keep rendering logic purely visual, with no chess rules in frontend

7. js/interaction.js
- Handles user interaction with the board
- First version should support two move methods:
  1. direct UCI text input + submit
  2. click source square, click target square
- Click interaction flow:
  - user clicks a square containing a piece
  - square becomes selected
  - user clicks target square
  - frontend constructs UCI move such as e2e4
  - frontend sends POST /game/move
  - on success refresh board and status
  - on error show backend error message and keep/reset selection
- Promotion may be deferred initially if not yet needed in UI, but code should be structured so promotion UI can be added later
- Keep selection state in frontend only:
  - selectedFromSquare
  - selectedToSquare (temporary)
- Use Three.js raycasting to detect clicked squares/pieces

8. js/ui.js
- Reads/writes regular DOM controls
- Handles:
  - New Game button
  - Reset Game button
  - Refresh button
  - dropdown values
  - status text
  - error messages
  - optional move history area if later added
- On New Game:
  - collect dropdown selections
  - build optional request body
  - call api.newGame(config)
  - rerender board + pieces + status
- On Reset:
  - call api.resetGame()
  - rerender
- On Refresh:
  - call api.getBoard() and api.getStatus()

9. js/main.js
- Frontend bootstrap entrypoint
- On page load:
  - initialize scene
  - create board
  - wire UI events
  - optionally call health()
  - show "Click New Game to start"
- The user must explicitly click New Game before play begins
- After New Game succeeds:
  - render pieces from returned state
  - fetch status if needed
  - enable move interactions

Required user flow
1. User starts backend server separately
2. User opens frontend in browser
3. Frontend loads and shows:
   - 3D empty or initialized board scene
   - controls panel
   - message: "Click New Game to start"
4. User optionally selects:
   - bot type
   - bot color
   - modeled side
5. User clicks New Game
6. Frontend sends POST /game/new with either:
   - empty body
   - or JSON body with selected config
7. Frontend receives GameStateResponse and renders:
   - board pieces
   - current side to move
   - phase
   - check/status indicators
8. User plays moves by:
   - entering UCI manually
   - or clicking source and target squares
9. After every successful move:
   - frontend rerenders pieces from returned board
   - refreshes status display
10. User may click Reset Game any time

UI requirements
Control panel must include:
- New Game button
- Reset Game button
- Refresh button
- Dropdown: Bot Type
  - empty / none
  - random
  - greedy
  - defensive
  - aggressive
- Dropdown: Bot Plays
  - empty / none
  - white
  - black
- Dropdown: Modeled Side
  - empty / none
  - white
  - black
- Text input for UCI move
- Submit Move button
- Status area showing:
  - sideToMove
  - phase.tag
  - isCheck
  - statusLine if available from /game/status
  - currentPlayerLine if available from /game/status
- Error area for backend messages

Board rendering requirements
- The board itself must be 3D
- Pieces can remain simple placeholders for now
- Camera must be orbitable with mouse using OrbitControls. :contentReference[oaicite:3]{index=3}
- Lighting should be sufficient so board and pieces are clearly visible
- Board squares should be individually clickable
- Selected square should be visibly highlighted
- Optional legal-move highlighting is not required because the backend already validates moves

Networking requirements
- Use fetch() for all requests. Fetch returns a promise resolving to a Response object; inspect `response.ok` and parse JSON accordingly. :contentReference[oaicite:4]{index=4}
- Use JSON request bodies for POST routes
- Set `Content-Type: application/json` for POST requests with a body
- Handle non-2xx responses by parsing ErrorResponse when possible
- Assume API is on `http://127.0.0.1:8080`
- If the frontend is served from another origin/port, backend may need CORS enabled because Fetch follows browser cross-origin rules. :contentReference[oaicite:5]{index=5}

Data mapping requirements
Map API board positions into 3D coordinates deterministically:
- Convert file a-h to x positions 0-7
- Convert rank 1-8 to z positions 0-7
- Place each square centered in a grid
- Place each piece mesh at the center of its square
- Keep y slightly above the board plane
- White and black pieces use different materials

Suggested helper mapping
- fileToIndex(file): a->0, b->1, ... h->7
- rankToIndex(rank): 1->0, ... 8->7
- squareToWorld(file, rank): returns x, y, z for mesh placement

Error handling requirements
Show readable UI errors for:
- backend not reachable
- malformed response
- invalid move format
- illegal move
- game already over
- invalid game creation config
Do not crash the scene if an API call fails.

Minimal visual design goals
- Functional first
- Clean layout
- One main 3D viewport
- One side control panel
- Placeholder geometry is acceptable
- No need for polished art assets yet

Non-goals for this task
- No React/Vue/Angular
- No WebSocket
- No multiplayer sessions
- No PGN/FEN import/export in UI yet
- No advanced piece animations required
- No external 3D chess asset pack required
- No AI chess logic in frontend
- No move legality calculation in frontend

Acceptance criteria
1. Opening the frontend shows a working 3D board scene.
2. User can click New Game and the frontend sends the correct POST /game/new request.
3. Optional dropdown values are included in POST /game/new only when selected.
4. Board pieces render from the returned GameStateResponse.
5. User can fetch and display current status.
6. User can make a move by entering UCI manually.
7. User can make a move by clicking source and target squares.
8. After a legal move, the board updates correctly from API response.
9. After an invalid/illegal move, an error message is shown in the UI.
10. Reset Game works and rerenders the initial position.
11. The app is usable with a bot-enabled game configuration.
12. The frontend contains no chess rules beyond building/sending move intents and rendering returned state.

Implementation notes
- Keep frontend state minimal:
  - currentBoardResponse
  - currentStatusResponse
  - selectedSquare
  - loading/error flags
- Keep Three.js concerns separate from API concerns
- Keep API concerns separate from DOM control concerns
- Build a vertical slice first:
  - scene
  - board
  - New Game
  - render pieces
  - move submission
  - click-to-move