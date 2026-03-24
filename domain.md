# Domain Model

## Purpose
This document defines the core domain types of the chess game and explains why they are modeled this way.

The goal is not to model full chess immediately, but to create a small, clean, testable foundation for iteration 1.

## Design Goals
- immutable state
- pure domain logic
- functional style
- simple TDD-friendly model
- minimal scope
- easy extension later

## Modeling Principles
- represent real domain concepts explicitly
- avoid invalid states where practical
- keep the UI separate from the domain
- avoid premature abstraction
- model only what is needed for the current iteration

## Core Types

### Color
Represents the side a piece belongs to and the current player.

enum Color:
  case White, Black

Why:
  - Useful for turn handling
  - Simple and type-safe

### pieceType
Represents the kind of a chess piece.

enum PieceType:
  case King, Queen, Rook, Bishop, Knight, Pawn

Why:
  - the set of valid piece kinds is fixed
  - enough for rendering and board setup
  - avoids unnecessary inheritance

### Piece

Represents a chess piece as a combination of color and type.

case class Piece(color: Color, kind: PieceType)

Why:

compact and expressive
easy to construct and test
enough for current requirements
simpler than a class hierarchy per piece

### Position

Represents a square on the chess board.

case class Position(file: Char, rank: Int)

Example:

Position('e', 2) represents e2

Why:

clearer than using raw strings like "e2"
makes parsing and validation easier
directly models a board coordinate

A validated constructor should be used:

object Position:
  def from(file: Char, rank: Int): Either[String, Position]

Why:

prevents invalid coordinates like z9
keeps validation at the system boundary

### Move

Represents a move from one position to another.

case class Move(from: Position, to: Position)

Why:

matches the current input format directly
keeps move handling simple
easy to parse and test

### Board

Represents the current placement of pieces on the board.

case class Board(pieces: Map[Position, Piece])

Why:

empty squares do not need to be stored explicitly
lookup by position is simple
immutable updates are straightforward
fits functional programming well
simpler than a mutable 2D array

Alternative representations like Vector[Vector[Option[Piece]]] are possible, but add indexing complexity without enough benefit for iteration 1.

### GameState

Represents the full current state of the game.

case class GameState(
  board: Board,
  currentPlayer: Color
)

Why:

one value represents the whole game
makes state transitions explicit
supports pure functions like applying a move
easy to test in TDD

### GameError

Represents expected domain and input errors.

enum GameError:
  case InvalidMoveFormat
  case InvalidPosition
  case NoPieceAtSource
  case WrongPlayer
  case SameColorCapture

Why:

better than returning raw strings everywhere
makes failures explicit
improves test precision
keeps domain logic typed and predictable




