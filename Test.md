# Testing Philosophy

## Goal
Use TDD to build a small, reliable, and fully tested chess game iteration.

The focus is not on testing Scala syntax or case classes themselves, but on testing:
- validated creation of domain values
- behavior of pure domain functions

## Core Principle
Tests should follow the shape of the system:
- parse input
- transform domain state
- render output

Most tests should target pure logic, not console I/O.

## Test Categories

### 1. Validation and Parsing Tests
These tests verify that domain values can be created correctly and invalid input is rejected safely.

Examples:
- `Position.from('e', 2)` returns a valid position
- `Position.from('z', 9)` returns an error
- `Move.fromString("e2e4")` returns a valid move
- `Move.fromString("e9e4")` returns an error

Why:
- this is where input correctness is enforced
- it prevents invalid states from entering the domain

### 2. Behavior and State Transition Tests
These tests verify what the system does with valid domain values.

Examples:
- `Board.initial` contains the expected pieces
- applying a move updates the board
- moving from an empty square fails
- a successful move switches the turn
- rendering produces the expected board string

Why:
- this is where the actual game logic lives
- these tests verify domain behavior instead of implementation details

## What Not to Focus On
Do not spend time testing trivial case class construction such as:

```scala
case class Piece(color: Color, kind: PieceType)