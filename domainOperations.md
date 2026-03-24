# Domain Operations

## Purpose
This document defines the core domain operations of the chess game.

The operations are designed in a strictly functional style:
- all required input is passed explicitly
- no global mutable state is used
- no side effects occur in domain logic
- functions return new values instead of mutating existing ones

## Design Principles
- functions should be small and focused
- inputs and outputs should be explicit
- errors should be represented with typed return values
- domain logic should remain independent from console I/O

## Position Operations

### Create Position

def createPosition(file: Char, rank: Int): Either[GameError, Position]

Expected behavior:
    - parses strings like e2 into a Position
    - rejects malformed or invalid coordinates
why:
    - user input enters the system as text
    - parsing should be isolated and testable

## Move Operations

### Create Move

def createMove(from: Position, to: Position): Move

Expected behavior:
    -creates a Move value from two valid positions

why:
    - a move is a pure domain value
    - no validation beyond valid positions is needed here

Parse Move
def parseMove(input: String): Either[GameError, Move]

Expected behavior:

parses strings like "e2e4" into a Move
returns Left(InvalidMoveFormat) if the format is invalid
returns Left(InvalidPosition) if one of the positions is invalid

Why:

keeps text parsing separate from game logic
makes move input testable in isolation
Board Operations
Initial Board
def initialBoard: Board

Expected behavior:

returns a board in the standard chess starting position

Why:

provides a deterministic and reusable starting point for a new game
Get Piece at Position
def pieceAt(board: Board, position: Position): Option[Piece]

Expected behavior:

returns Some(piece) if a piece exists at the given position
returns None if the square is empty

Why:

querying the board should be explicit and pure
avoids hidden global state
Place Piece
def placePiece(board: Board, position: Position, piece: Piece): Board

Expected behavior:

returns a new board with the given piece placed at the given position

Why:

useful as a small primitive operation
keeps board transformations explicit
Remove Piece
def removePiece(board: Board, position: Position): Board

Expected behavior:

returns a new board without a piece at the given position

Why:

useful as a primitive for move application
avoids mutation
Relocate Piece
def relocatePiece(board: Board, move: Move): Board

Expected behavior:

removes the piece from the source square
places it on the target square
overwrites any piece on the target square

Why:

represents the mechanical board update only
does not enforce turn rules or move legality
Color Operations
Opposite Color
def opposite(color: Color): Color

Expected behavior:

returns Black for White
returns White for Black

Why:

used when switching turns
small pure domain helper
Game State Operations
Initial Game State
def initialGameState: GameState

Expected behavior:

returns a GameState with the initial board and White as current player

Why:

provides a clean entry point for starting a game
Apply Move
def applyMove(state: GameState, move: Move): Either[GameError, GameState]

Expected behavior:

fails if the source square is empty
fails if the source piece belongs to the wrong player
fails if the target square contains a piece of the same color, if that rule is included
otherwise:
relocates the piece
returns a new GameState
switches the current player

Why:

this is the central domain state transition
all move application logic should remain pure and explicit
Rendering Operations
Render Piece
def renderPiece(piece: Piece): Char

Expected behavior:

returns a character representation of a piece
example: white pawn -> P, black pawn -> p

Why:

keeps board rendering simple
isolates symbol logic
Render Board
def renderBoard(board: Board): String

Expected behavior:

returns a deterministic multiline string representation of the board
includes all ranks and files
shows pieces and empty squares

Why:

rendering should be testable without console I/O
visible game output should come from pure functions
Render Current Player
def renderCurrentPlayer(color: Color): String

Expected behavior:

returns strings like "White to move" or "Black to move"

Why:

separates textual presentation from core state logic