# Simple Chess Game – Iteration 1 Specification

## Goal
Implement a simple console-based chess game in pure Scala using a functional style.

## Scope
This iteration focuses on a minimal vertical slice:
- initialize a standard chess board
- render the board in the console
- accept moves in text form
- parse moves
- apply moves to the board
- alternate turns
- provide automated tests with full coverage for the implemented scope

## Out of Scope
The following are not required yet:
- legal move validation by piece type
- check, checkmate, stalemate
- castling
- en passant
- pawn promotion
- AI opponent
- GUI
- persistence
- networking

## Functional Requirements

### Board
- The game shall represent an 8x8 chess board.
- Each square shall either be empty or contain exactly one piece.
- Pieces shall have a color (`White`, `Black`) and a type (`King`, `Queen`, `Rook`, `Bishop`, `Knight`, `Pawn`).
- The board shall be immutable.

### Initial State
- The game shall provide the standard chess starting position.
- White shall move first.

### Input
- The game shall accept moves in the format `e2e4`.
- A move consists of a source square and a target square.
- Invalid input shall be rejected safely.

### Parsing
- Valid move strings shall be parsed into an internal move representation.
- Invalid move strings or invalid coordinates shall return an error, not crash the program.

### Move Application
- A move shall fail if the source square is empty.
- A move shall fail if the source piece belongs to the other player.
- A successful move shall move the piece from source to target.
- The source square shall become empty.
- If a piece exists on the target square, it shall be replaced.
- The system does not need to validate whether the move is legal by chess rules.

### Turn Handling
- White moves first.
- After a successful move, the turn shall switch.
- After a failed move, the turn shall remain unchanged.

### Rendering
- The board shall be rendered as deterministic plain text.
- White and Black pieces shall be visually distinguishable.
- Empty squares shall be shown explicitly.

### Console Loop
- The application shall print the board.
- The application shall show the current player.
- The application shall read move input from the console.
- The application shall apply valid moves or show an error.
- The application shall continue until the user exits.

## Non-Functional Requirements

### Style
- The implementation shall use pure Scala.
- The implementation shall follow a functional style where reasonable:
  - immutable data
  - pure domain logic
  - side effects isolated to the console layer

### Structure
- The code shall separate:
  - domain model
  - parsing
  - rendering
  - console I/O

### Testing
- The implementation shall be developed with TDD.
- Automated tests shall cover all implemented functionality.
- The target is 100% code coverage for the implemented scope.

## Acceptance Criteria
The iteration is complete when:
1. the initial board can be created
2. the board can be rendered in text form
3. valid move input like `e2e4` can be parsed
4. invalid move input is rejected safely
5. pieces can be moved from source to target
6. moving from an empty square fails
7. moving the wrong player’s piece fails
8. turns alternate after successful moves
9. turns do not change after failed moves
10. the console loop supports repeated input
11. tests exist for all implemented behavior
12. implemented code reaches full coverage

## Explicit Simplifications
For this iteration, the following is allowed:
- moves may be illegal in real chess
- pieces do not need piece-specific move rules
- kings may move into check
- pawns may move backwards
- no game-end detection is required