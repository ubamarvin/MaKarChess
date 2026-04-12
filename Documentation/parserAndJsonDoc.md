# Parser and JSON Architecture Documentation

This document explains where the FEN/PGN parsers and JSON serialization are implemented, how they are structured, and how they are wired into the application.

## Overview

The project now separates parsing and serialization from the chess domain logic.
The domain model remains in `makarchess.model`, while parsing and JSON handling are organized into dedicated modules.

Main goals of the structure:
- interchangeable parser implementations
- separation of parser logic from chess rules
- dependency injection through the composition root
- reusable shared transformation logic
- isolated JSON serialization behind a codec interface

## Parser API layer

The parser API lives in:

- `src/main/scala/makarchess/parser/api/ChessParser.scala`
- `src/main/scala/makarchess/parser/api/FenParser.scala`
- `src/main/scala/makarchess/parser/api/PgnParser.scala`
- `src/main/scala/makarchess/parser/api/ParserBackend.scala`

### Purpose

These files define the interfaces the rest of the application should depend on.

- `ChessParser[A]`
  - generic parser contract with `parse(input: String): Either[String, A]`

- `FenParser`
  - parses `String -> Either[String, Fen]`
  - renders `Fen -> String`

- `PgnParser`
  - parses `String -> Either[String, PgnGame]`
  - renders `PgnGame -> String`

- `ParserBackend`
  - enum used to select which concrete parser family should be used
  - values: `Fast`, `Combinator`, `Regex`

## Compatibility traits

For compatibility with existing code, the older parser traits still exist in:

- `src/main/scala/makarchess/parser/Parser.scala`
- `src/main/scala/makarchess/parser/FenParser.scala`
- `src/main/scala/makarchess/parser/PgnParser.scala`

These traits now extend the new API traits so existing imports continue to work.

## Shared parser logic

Common transformation and rendering logic is centralized in:

- `src/main/scala/makarchess/parser/shared/FenSupport.scala`
- `src/main/scala/makarchess/parser/shared/PgnSupport.scala`

### `FenSupport`

`FenSupport` contains the logic that should not be duplicated across the three FEN parser implementations.

It is responsible for:
- converting parsed FEN fields into the `Fen` domain object
- validating board layout, side to move, castling, en passant, counters
- rendering a `Fen` object back into a FEN string

### `PgnSupport`

`PgnSupport` contains reusable PGN transformation logic.

It is responsible for:
- building a `PgnGame` from parsed tag/value pairs and raw move text
- tokenizing PGN move text
- removing comments and variations
- detecting game results
- rendering a `PgnGame` back to PGN text

This design keeps the frontend parser technology separate from the actual conversion rules.

## FEN parser implementations

The three FEN parser implementations are located in:

- `src/main/scala/makarchess/parser/fen/FastFenParser.scala`
- `src/main/scala/makarchess/parser/fen/CombinatorFenParser.scala`
- `src/main/scala/makarchess/parser/fen/RegexFenParser.scala`

### 1. `FastFenParser`

Uses the `fastparse` library.

Responsibilities:
- parse the six FEN fields using FastParse rules
- delegate object creation and validation to `FenSupport.buildFen`
- render through `FenSupport.renderFen`

### 2. `CombinatorFenParser`

Uses `scala-parser-combinators`.

Responsibilities:
- parse the same six FEN fields with parser combinators
- delegate validation/building to `FenSupport.buildFen`
- render through `FenSupport.renderFen`

### 3. `RegexFenParser`

Uses a regular expression to split the high-level FEN structure.

Responsibilities:
- match the six top-level FEN fields
- delegate validation/building to `FenSupport.buildFen`
- render through `FenSupport.renderFen`

## PGN parser implementations

The three PGN parser implementations are located in:

- `src/main/scala/makarchess/parser/pgn/FastPgnParser.scala`
- `src/main/scala/makarchess/parser/pgn/CombinatorPgnParser.scala`
- `src/main/scala/makarchess/parser/pgn/RegexPgnParser.scala`

### 1. `FastPgnParser`

Uses the `fastparse` library.

Responsibilities:
- parse the optional tag section
- extract raw move text
- delegate game construction to `PgnSupport.buildGame`
- render through `PgnSupport.renderPgn`

### 2. `CombinatorPgnParser`

Uses `scala-parser-combinators`.

Responsibilities:
- parse optional tags with combinator rules
- parse multiline move text
- delegate game construction to `PgnSupport.buildGame`
- render through `PgnSupport.renderPgn`

### 3. `RegexPgnParser`

Uses line-based regex matching for PGN tags.

Responsibilities:
- separate tag lines from move text lines
- parse tag lines with regex
- delegate game construction to `PgnSupport.buildGame`
- render through `PgnSupport.renderPgn`

## Parser factory / backend selection

The factory for selecting parser implementations is:

- `src/main/scala/makarchess/parser/ParserModule.scala`

### Purpose

`ParserModule` maps a `ParserBackend` value to a concrete implementation.

It exposes:
- `fenParser(backend: ParserBackend): FenParser`
- `pgnParser(backend: ParserBackend): PgnParser`

This is the main indirection layer that allows the application to switch parser technology without changing consumer code.

## Legacy FastParse wrappers

The older wrapper classes still exist in:

- `src/main/scala/makarchess/parser/fastparse/FastParseFenParser.scala`
- `src/main/scala/makarchess/parser/fastparse/FastParsePgnParser.scala`

These now delegate to the new implementations in:
- `makarchess.parser.fen.FastFenParser`
- `makarchess.parser.pgn.FastPgnParser`

They were kept so older imports do not break immediately.

## Replay and navigation

PGN replay logic is located in:

- `src/main/scala/makarchess/model/PgnReplay.scala`
- `src/main/scala/makarchess/model/PgnReplayCursor.scala`

### `PgnReplay`

`PgnReplay` is responsible for resolving SAN move tokens against legal moves from the current chess state.

It provides:
- `buildCursor(startState, moves)`
- `toFinalState(startState, moves)`
- SAN normalization and SAN matching helpers

### `PgnReplayCursor`

`PgnReplayCursor` stores all intermediate states of a replay in a `Vector[ChessState]`.

It supports:
- `currentState`
- `stepForward()`
- `stepBackward()`
- `jumpToStart()`
- `jumpToEnd()`
- `jumpTo(index)`

This makes backward navigation efficient because every state is already stored.

## JSON codec layer

The JSON codec abstraction is in:

- `src/main/scala/makarchess/serialization/GameStateJsonCodec.scala`

The concrete implementation is in:

- `src/main/scala/makarchess/serialization/UpickleGameStateJsonCodec.scala`

### Purpose

This layer isolates JSON details from the rest of the application.
Consumers only depend on `GameStateJsonCodec`, not on `uPickle` directly.

### `UpickleGameStateJsonCodec`

This class handles:
- encoding `ChessState` to JSON
- decoding JSON back into `ChessState`
- mapping nested domain types like:
  - `Color`
  - `PieceType`
  - `Position`
  - `Piece`
  - `CastlingRights`
  - `PositionKey`
  - `GamePhase`

The top-level `ChessState` conversion is implemented explicitly to keep the JSON structure stable and easy to control.

## JSON persistence service

The persistence adapter for game state JSON is:

- `src/main/scala/makarchess/persistence/GameStateJsonService.scala`

### Purpose

`GameStateJsonService` connects:
- `FileIO`
- `GameStateJsonCodec`

It provides:
- `save(state: ChessState)`
- `load()`

By default it uses the file path:
- `gamestate.json`

That means the state is stored at the project root unless another path is passed in.

## File-based parser services

File-based parsing still uses dedicated services:

- `src/main/scala/makarchess/persistence/FenFileService.scala`
- `src/main/scala/makarchess/persistence/PgnFileService.scala`

These services combine:
- `FileIO`
- a parser interface

So file reading is still separated from parsing.

## Dependency injection and wiring

The composition root is:

- `src/main/scala/makarchess/Main.scala`

The controller wiring is in:

- `src/main/scala/makarchess/controller/ChessController.scala`

### In `Main`

`Main` creates:
- `LocalFileIO`
- selected FEN parser via `ParserModule.fenParser(ParserBackend.Fast)`
- selected PGN parser via `ParserModule.pgnParser(ParserBackend.Fast)`
- `GameStateJsonService`

These are then passed into `ChessController`.

### In `ChessController`

`ChessController` depends on abstractions/services instead of hard-coded parser logic.
It now supports:
- loading FEN from string/file
- loading PGN from string/file
- parsing PGN without applying it
- building a PGN replay cursor from text
- saving current state to JSON
- loading state from JSON

## Domain separation

The parser module does not contain chess rules.
Chess legality, move application, and game-state evaluation remain in:

- `src/main/scala/makarchess/model/ChessRules.scala`
- `src/main/scala/makarchess/model/ChessModel.scala`

This keeps the domain pure and prevents the parser implementations from owning game logic.

## Tests

The main new tests for this architecture are in:

- `src/test/scala/ParserArchitectureSpec.scala`
- `src/test/scala/GameStateJsonServiceSpec.scala`

These tests verify:
- all parser backends produce the same parsed result
- FEN render/parse roundtrip
- PGN render/parse roundtrip
- PGN replay navigation
- JSON roundtrip
- JSON file persistence

## Summary

The parser and JSON design follows this structure:

- interfaces in `parser.api` and `serialization`
- concrete parsers in `parser.fen` and `parser.pgn`
- shared logic in `parser.shared`
- replay logic in `model`
- persistence adapters in `persistence`
- selection and DI wiring in `ParserModule`, `Main`, and `ChessController`

This allows the project to switch parser implementations without changing consumer code, while keeping parsing, replay, serialization, and chess rules clearly separated.
