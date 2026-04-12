Refactor the existing Scala 3 chess project so parsing becomes a separate, swappable module and persistence/web serialization is prepared cleanly.

Current context
- Core chess domain types already exist: Color, PieceType, Piece, Position, CastlingRights, Move, ChessState, GamePhase. :contentReference[oaicite:0]{index=0}
- There is already a FEN data structure plus conversion to ChessState, but it is not yet organized as an injectable parser module. :contentReference[oaicite:1]{index=1}
- There is already a PGN model plus replay logic, but it is not yet organized as an injectable parser/navigation module. 
- The current model still directly depends on chess rules/state and should not depend on concrete parser implementations. 

Goals
1. Move parsing into its own module with interfaces only exposed to the rest of the application.
2. Provide three interchangeable parser implementations for both FEN and PGN:
   - fast parser
   - parser combinator parser
   - regex parser
3. Wire everything via dependency injection, so the app can choose implementation without changing consumer code.
4. Add JSON serialization/deserialization for game state, for now, save the gamestate.json in a file located at the project root
   - ChessState -> JSON
   - JSON -> ChessState
5. Prepare persistence/web UI integration cleanly, but do not implement the DB adapter yet.
6. Extend PGN support so replay supports both forward and backward navigation through the game.

Architectural requirements
- Keep domain pure and independent.
- Introduce a dedicated parser module/package, for example:
  - makarchess.parser.api
  - makarchess.parser.fen
  - makarchess.parser.pgn
  - makarchess.serialization
- The rest of the app must depend only on traits/interfaces, never on concrete parser classes.
- Use constructor injection or module wiring, no hidden globals or hard-coded parser selection.
- Keep parsing concerns separate from chess rules and model state transitions.

Required interfaces
Create traits similar to these:

- trait FenParser:
  - def parse(input: String): Either[String, Fen]
  - def render(fen: Fen): String

- trait PgnParser:
  - def parse(input: String): Either[String, PgnGame]
  - def render(game: PgnGame): String

- trait GameStateJsonCodec:
  - def encode(state: ChessState): Either[String, String]
  - def decode(json: String): Either[String, ChessState]

Also create factory/wiring abstractions so consumers can request:
- fast FEN parser
- combinator FEN parser
- regex FEN parser
- fast PGN parser
- combinator PGN parser
- regex PGN parser

PGN replay/navigation
Design a replay/navigation component that:
- accepts an initial ChessState and a parsed PgnGame
- resolves the move list into a navigable sequence of positions
- supports:
  - current state
  - stepForward()
  - stepBackward()
  - jumpToStart()
  - jumpToEnd()
  - jumpTo(index)
- stores all intermediate states so backward navigation is efficient
- returns Either/String errors if PGN cannot be resolved legally

Suggested type:
- case class PgnReplayCursor(states: Vector[ChessState], index: Int)

JSON requirements
- Implement JSON codec for ChessState and all required nested types
- Preserve enough information for persistence and web UI:
  - board
  - sideToMove
  - castling
  - enPassant
  - halfmoveClock
  - fullmoveNumber
  - phase
  - optionally repetitionHistory if needed for correctness
- Use a clean adapter/codec layer, not ad-hoc string building scattered across the app
- Choose one Scala JSON library and keep it isolated behind GameStateJsonCodec

Implementation details
- Reuse existing domain types where possible instead of redefining them. :contentReference[oaicite:4]{index=4}
- Reuse existing Fen and PgnGame models if suitable. 
- Reuse ideas from current PGN replay logic, but redesign it into a proper navigation component instead of only “to final state”. :contentReference[oaicite:6]{index=6}
- Do not break existing chess rules logic.
- Do not couple parser implementations to UI, controller, database, or observer code.

Deliverables
1. New parser module structure
2. Traits/interfaces for FEN, PGN, and JSON codec
3. Three FEN parser implementations:
   - FastFenParser
   - CombinatorFenParser
   - RegexFenParser
4. Three PGN parser implementations:
   - FastPgnParser
   - CombinatorPgnParser
   - RegexPgnParser
5. Replay/navigation component for PGN with forward/backward stepping
6. JSON codec for ChessState
7. Dependency-injection based wiring example
8. Unit tests for:
   - each parser implementation
   - same input produces same parsed result across implementations
   - FEN roundtrip
   - PGN parse + replay
   - PGN forward/backward navigation
   - ChessState JSON roundtrip

Non-goals
- No database adapter yet
- No web UI yet
- No unnecessary GUI work
- No rewriting of the chess engine unless required for clean integration

Quality bar
- Scala 3
- clean module boundaries
- small, focused files
- testable design
- no consumer should know whether parsing is done by fastparse, combinators, or regex