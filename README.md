## sbt project compiled with Scala 3

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).

---

## Bots

You can enable a chess bot for one side via CLI flags:

- `--bot=white` or `--bot=black`
- `--bot-type=random|greedy|defensive|aggressive` (default: `random`)

Examples:

```bash
sbt "run -- --bot=black"
sbt "run -- --bot=black --bot-type=greedy"
```

---

## Opponent Modeling

This project includes a planned opponent modeling subsystem (see `specs/Task2OponnentModelingSpec.md`).

### Goal

The goal is to infer *style tendencies* from observed moves (not hard-identify a concrete bot class) and use that to:

- derive a human-readable style estimate (aggressive/greedy/defensive/risk tolerance),
- predict likely next moves,
- highlight likely destination squares directly in the TUI.

### Architecture (Pipeline)

The subsystem is designed as a small, testable pipeline of pure components and immutable data:

- **Observation**
  - observed move + board state context
- **Feature extraction** (`MoveFeatures`)
  - e.g. capture, check, moves into danger, saves own piece, king pressure
- **Profile update** (`OpponentProfile`)
  - rolling evidence values + confidence, updated incrementally after each modeled move
- **Style estimate** (`StyleEstimate`)
  - display-friendly percentages derived from the internal profile evidence
- **Prediction** (`PredictionResult`)
  - ranked legal moves (`PredictedMove`) + UI highlights (`HighlightSquare`)

Runtime wiring follows the existing MVC + Observer pattern:

- `OpponentModelObserver` subscribes to model updates.
- It maintains `OpponentProfile` internally (not inside the chess model).
- After each move by the configured modeled color, it updates profile + computes predictions.
- The TUI only renders `HighlightSquare` data (no inference logic in the view).

### CLI Flags

Opponent modeling is enabled by selecting which side to model:

- `--model=white`
- `--model=black`

The minimum number of observed moves required before predictions and style are shown can be configured via:

- `--model-min-moves=<n>` (default: `5`)

If `--model` is omitted, opponent modeling is disabled.

Examples:

```bash
sbt "run -- --model=black"
sbt "run -- --bot=black --bot-type=aggressive --model=black"
sbt "run -- --bot=black --bot-type=aggressive --model=black --model-min-moves=0"
```

### TUI Highlights

Predicted destination squares are rendered as numeric labels on the board:

- `1` = most likely
- `2` = likely
- `3` = maybe
