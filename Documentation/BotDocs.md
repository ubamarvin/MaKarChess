# Bot documentation

## Overview
The bot implementation follows the existing **MVC + Observer** architecture.

- **Model**
  - Owns the immutable game state (`ChessState`) and chess rules.
  - Exposes a read-only `chessState` accessor on `ChessModel` so external components (like bots) can inspect the full state.

- **Controller**
  - Owns the currently active immutable `ChessModel` instance.
  - Applies moves through the existing flow (`handleMoveInput` / `makeMove`).
  - Calls `notifyObservers` after successful state changes.

- **View (TUI)**
  - Is an `Observer`.
  - Re-renders by reading a fresh `snapshot` from the controller.

- **Bot**
  - **Pure decision component**: given the current `ChessState`, it returns `Option[Move]`.

- **BotCaller**
  - **Orchestrator** component.
  - Is an `Observer` that subscribes to model updates.
  - When the model changes, it checks whether it is currently the bot’s turn.
  - If it is the bot’s turn, it asks the bot for one move and executes that move through the controller.

## Architectural wiring (Observer flow)

### Relevant files
- `src/main/scala/makarchess/Util/Bot/BotTrait.scala`
  - `trait Bot` with `chooseMove(state: ChessState): Option[Move]`

- `src/main/scala/makarchess/Util/Bot/BotRandom.scala`
  - `RandomBot` implementation
  - Uses `ChessRules.legalMoves(state)` and selects one move randomly.

- `src/main/scala/makarchess/Util/Bot/BotCaller.scala`
  - `BotCaller(controller, bot, botPlays) extends Observer`
  - Subscribes via `controller.model.add(this)`
  - On update:
    - checks game is still in progress
    - checks `sideToMove == botPlays`
    - calls `controller.makeMove(...)` exactly once

- `src/main/scala/makarchess/Main.scala`
  - Parses CLI args `--bot=white` / `--bot=black`
  - Instantiates `BotCaller` when bot mode is enabled

### Sequence diagram (conceptual)
1. **Human** enters a move in TUI.
2. **TUI** calls `controller.handleMoveInput(...)`.
3. **Controller** updates its `currentModel` (immutable model replacement) and then calls `currentModel.notifyObservers`.
4. **TUI** receives `update` and re-renders.
5. **BotCaller** receives the same `update`.
6. If it is the bot’s turn:
   - `BotCaller` calls `bot.chooseMove(controller.model.chessState)`.
   - If it gets `Some(move)`, it executes it via `controller.makeMove(move.from, move.to, move.promotion)`.
   - Controller updates model and notifies observers again.

`BotCaller` includes a small `isExecuting` guard to prevent repeated triggering loops inside observer updates.

## How to run Human vs Bot

From the project root:

- Bot plays **Black** (human plays White):

```bash
sbt "run --bot=black"
```

- Bot plays **White** (human plays Black):

```bash
sbt "run --bot=white"
```

If you run without flags, the game starts in normal (no-bot) mode:

```bash
sbt run
```

## Notes
- The bot never bypasses legality checks: it chooses from `ChessRules.legalMoves(state)`.
- The bot never mutates the model directly: it always executes moves through the controller.
- The implementation is intentionally simple so stronger bots can be added later by implementing the same `Bot` trait.
