# Specification: Opponent Modeling Data Model, Prediction Pipeline, and TUI Highlight Integration

## Purpose

This specification defines the **data model and architectural foundation** for the next feature of the chess project: **opponent modeling**.

The purpose of opponent modeling in this project is **not primarily to identify the exact concrete bot class** that is being used by the opponent. Instead, the primary goal is to infer the opponent’s **current behavioral tendencies and style profile** from observed moves, and then use that profile to:

1. describe how the opponent is currently playing,
2. predict likely future moves,
3. expose this information to the TUI in a visible way,
4. later allow adaptation of the player’s own bot behavior.

For proof-of-concept purposes, the system should still be capable of recognizing a **pure archetypal bot** such as a purely aggressive bot and producing an estimate close to:

- aggressive = 100%
- greedy = 0%
- defensive = 0%

However, this is considered a **special case of style inference**, not the core abstraction.

The goal of this specification is to define the exact responsibilities and structure for:

- `OpponentProfile`
- `StyleEstimate`
- `PredictionResult`
- TUI highlight integration

and the supporting types that make these components useful and testable.

---

# Decisions / Clarifications (Project-Specific)

This section records concrete integration decisions for this project so the implementation is unambiguous.

## Modeled Side is Configurable (CLI)

The system must support modeling either **White** or **Black**.

- The modeled color is provided via a command-line argument.
- The modeled color may represent the opponent or the player themselves; the system must remain flexible.

### CLI Flag Name

The modeled side is configured via:

- `--model=white`
- `--model=black`

If `--model` is not provided, opponent modeling is disabled.

### CLI Usage Examples

Run without any bot, but with modeling enabled:

```bash
sbt "run --model=black"
```

Run with a bot and modeling enabled:

```bash
sbt "run --bot=black --bot-type=aggressive --model=black"
```

## Observer-Based Integration

The opponent modeling subsystem is integrated into the running game via a dedicated observer:

- `OpponentModelObserver` subscribes to the model update notifications.
- It maintains the rolling `OpponentProfile` internally (not inside the chess model).
- It only updates the profile when the **modeled color** just made a move.

### Construction / Wiring

`OpponentModelObserver` must be created in `Main` when `--model=...` is present, similarly to how bots are wired in.

## TUI Highlight Rendering as Numeric Labels

Highlights are rendered on the board as numeric labels:

- `"1"` = most likely target square
- `"2"` = likely
- `"3"` = maybe
- and so on (if more than 3 highlights are shown)

These numbers must appear on the destination squares of the top ranked predicted moves.

---

# High-Level Design Goal

The opponent modeling system should be designed in a way that remains valid even as many more bots are added in the future.

This means the architecture should be based on **stable style dimensions** rather than hard-coding logic around concrete bot names.

Examples of stable style dimensions:

- aggression
- greed
- defensiveness
- risk tolerance
- king pressure
- tactical sharpness
- simplification tendency

Concrete bot types such as:

- `AggressiveBot`
- `GreedyBot`
- `DefensiveBot`
- future `PositionalBot`
- future `SacrificialBot`
- future `MinimaxBot`

should be viewed as **possible sources of behavior**, but not as the primary modeling abstraction.

Therefore, the opponent modeling subsystem must be designed around the question:

> “What tendencies does the opponent currently show, and what does that imply about their likely next move?”

rather than:

> “Which exact bot implementation is this?”

---

# Scope of This Feature

This feature covers:

- storing observed opponent tendencies,
- deriving a human-readable style estimate,
- producing ranked move predictions,
- exposing board-square highlights to the TUI.

This feature does **not yet** include:

- adapting the player’s own bot based on the model,
- perfect or exact bot identity classification,
- machine learning,
- persistent storage across games,
- advanced statistical modeling,
- hidden-information inference,
- non-heuristic search over all possible bot implementations.

This is intentionally a **heuristic and explainable** design.

---

# Architectural Principles

The implementation must follow these design principles.

## 1. Separate Internal State from Display State

The internal opponent model should store evidence and rolling state in a form suitable for continuous updates.

The display layer should use a separate derived structure that is easier to print in the TUI or logs.

This means the project should distinguish between:

- internal model state
- derived style representation for display
- predicted move rankings
- UI highlight data

These must not be collapsed into one giant structure.

---

## 2. Keep the Model Incremental

The opponent model must update after every observed opponent move.

It must **not** be treated as something that is “filled once” after a few moves and then frozen.

Instead, it should behave as a rolling, continuously updated belief about the opponent.

This is important because an opponent’s behavior may shift over the course of the game:

- opening
- middlegame
- endgame
- tactical sequence
- defensive recovery
- simplification phase

Therefore the model must remain updateable throughout the game.

---

## 3. Keep It Explainable

The model should be interpretable by a developer and understandable during debugging.

It should be possible to inspect:

- why the aggression score increased,
- why the greed score decreased,
- why a move was predicted,
- why a square was highlighted.

The design should avoid opaque structures that are hard to reason about.

---

## 4. Keep It Testable

All core data structures and transformations should be testable in isolation.

This includes:

- feature extraction,
- profile updates,
- style estimate derivation,
- move prediction ranking,
- square highlight generation.

---

# Conceptual Layers

The opponent modeling system should be structured into the following conceptual layers:

1. **Observation layer**  
   Raw observed opponent move and game state context.

2. **Feature layer**  
   Extracted move characteristics such as capture, check, self-risk, king pressure.

3. **Profile layer**  
   Long-lived rolling state storing accumulated evidence about the opponent.

4. **Estimate layer**  
   Human-readable style interpretation such as 80% aggressive.

5. **Prediction layer**  
   Ranked likely next moves and corresponding highlighted squares.

6. **UI integration layer**  
   TUI rendering of likely opponent target squares.

This specification focuses mainly on layers 3 to 6, but it also defines one supporting feature type because it is necessary for the update pipeline.

---

# Required Data Types

The implementation must define the following core data structures:

- `MoveFeatures`
- `OpponentProfile`
- `StyleEstimate`
- `PredictedMove`
- `HighlightSquare`
- `PredictionResult`

These are described below.

---

# 1. MoveFeatures

## Purpose

`MoveFeatures` represents the extracted behavioral signals from a single observed opponent move.

This type is an intermediate structure between:

- the raw move and state,
- the long-lived opponent profile.

It exists so that profile updating does not need to directly inspect game state in an ad hoc way.

Without this type, the update logic would become overly coupled and hard to test.

## Responsibilities

`MoveFeatures` should capture what the observed move says about the opponent’s behavior.

Typical examples include:

- whether the move was a capture,
- whether it gave check,
- whether it moved into danger,
- whether it saved an own piece,
- whether it increased pressure on the enemy king,
- whether it improved own safety,
- whether it looked materially greedy,
- whether it looked aggressive,
- whether it looked defensive.

## Required Structure

A concrete structure like the following is required:

```scala
final case class MoveFeatures(
  isCapture: Boolean,
  captureValue: Int,
  givesCheck: Boolean,
  movesIntoDanger: Boolean,
  savesOwnPiece: Boolean,
  increasesKingPressure: Boolean
)

This is the minimal recommended shape.

The implementation may extend this with more fields later, such as:

improvesCenterControl: Boolean
isRetreat: Boolean
isDevelopingMove: Boolean
isTradeOffer: Boolean
improvesKingSafety: Boolean
reducesOwnRisk: Boolean

The important rule is:

MoveFeatures should represent extracted move-level evidence, not long-term interpretation.

Non-Goals of MoveFeatures

MoveFeatures must not:

store rolling evidence across moves,
store UI display percentages,
know anything about TUI rendering,
directly predict future moves.

It is purely an intermediate analysis object for one move.

2. OpponentProfile
Purpose

OpponentProfile is the long-lived internal state of the opponent model.

It stores the accumulated evidence gathered so far from the opponent’s observed moves.

This is the core internal representation from which style estimates and predictions are derived.

It is not primarily meant for direct display.

Key Role

OpponentProfile answers the question:

“Based on everything seen so far, what tendencies does this opponent appear to have?”

It should be designed to support continuous updates after every opponent move.

Recommended Structure

The recommended structure is:

final case class OpponentProfile(
  observedMoves: Int,
  aggressionEvidence: Double,
  greedEvidence: Double,
  defensiveEvidence: Double,
  riskEvidence: Double,
  confidence: Double
)
Field Semantics
observedMoves: Int

The number of opponent moves that have been analyzed and incorporated into the profile.

Purpose:

allows confidence growth based on evidence count,
supports testing convergence,
makes logs and debugging easier.
aggressionEvidence: Double

Rolling internal evidence that the opponent prefers aggressive behavior.

This should increase when the opponent frequently chooses moves such as:

checks,
king pressure,
direct attacks,
forcing play,
attacking enemy pieces.

This field is internal evidence, not yet a percentage.

greedEvidence: Double

Rolling internal evidence that the opponent prefers immediate material gain.

This should increase when the opponent frequently chooses moves such as:

captures,
high-value captures,
material-winning trades,
obvious tactical grabs.

Again, this is internal evidence, not a display percentage.

defensiveEvidence: Double

Rolling internal evidence that the opponent prioritizes safety and preservation.

This should increase when the opponent frequently chooses moves such as:

rescuing attacked pieces,
retreating to safer squares,
covering own pieces,
reducing threats against themselves,
improving king safety.
riskEvidence: Double

Rolling internal evidence that the opponent tolerates or prefers risky play.

This should increase when the opponent frequently chooses moves such as:

entering attacked squares,
sacrificing safety for initiative,
accepting tactical danger,
leaving own pieces exposed for pressure.

This field is intentionally separate from aggression because an opponent can be:

aggressive and risky,
aggressive but precise,
defensive but occasionally risky,
greedy without being aggressive.
confidence: Double

A measure of how much trust should currently be placed in the profile.

This must reflect that early observations are weak evidence.

For example:

after 1 move, confidence should be low,
after 10 consistent moves, confidence should be much higher.

The confidence value should not merely be a function of move count. It may also consider consistency of behavior.

Important Design Rule

OpponentProfile should store evidence, not already normalized percentages.

Why this is better:

updates are easier to reason about,
recent-move weighting becomes easier,
normalization can be changed later without changing storage,
display derivation remains separate.
Update Model Requirement

OpponentProfile must be designed for rolling updates.

A future updater should be able to do something like:

updatedProfile = OpponentProfileUpdater.update(profile, features)

This means OpponentProfile must remain immutable and fully replaceable.

No mutable global state should be required.

Non-Goals of OpponentProfile

OpponentProfile must not:

directly contain TUI formatting concerns,
directly contain ranked predictions,
directly contain board highlights,
assume a specific concrete bot type,
hard-code exact bot identity.

It is an internal style-evidence container.

3. StyleEstimate
Purpose

StyleEstimate is the human-readable and display-ready interpretation of an OpponentProfile.

It answers the question:

“How should we currently describe this opponent’s style?”

Unlike OpponentProfile, this type is intended for:

TUI display,
logs,
debugging output,
proof-of-concept evaluation.
Recommended Structure
final case class StyleEstimate(
  aggressivePct: Double,
  greedyPct: Double,
  defensivePct: Double,
  riskTolerancePct: Double,
  confidencePct: Double
)
Field Semantics
aggressivePct

Display percentage for current estimated aggression tendency.

Example:

90.0 means the opponent currently appears strongly aggressive.
greedyPct

Display percentage for current estimated greed/materialism.

defensivePct

Display percentage for current estimated defensiveness/safety preference.

riskTolerancePct

Display percentage for current estimated willingness to accept tactical danger.

confidencePct

Display percentage for how strongly the model trusts the estimate.

This is crucial because:

“90% aggressive after 2 moves” is weak evidence,
“90% aggressive after 15 consistent moves” is much more meaningful.
Normalization Requirement

StyleEstimate should be derived from OpponentProfile through a dedicated estimator component.

For example:

StyleEstimator.fromProfile(profile): StyleEstimate

The percentages should be stable and display-friendly.

The exact normalization formula may vary, but it must be consistent and testable.

Why This Type Exists Separately

StyleEstimate must be separate from OpponentProfile because internal evidence and display percentages are different concerns.

This separation allows:

better encapsulation,
cleaner UI code,
easier evolution of update formulas,
easier testing of profile-vs-display logic.
Proof-of-Concept Requirement

For proof-of-concept scenarios, a pure archetypal opponent such as a fully aggressive bot should eventually produce a StyleEstimate close to:

StyleEstimate(
  aggressivePct = 100.0,
  greedyPct = 0.0,
  defensivePct = 0.0,
  riskTolerancePct = some reasonable value,
  confidencePct = high
)

Exact values do not need to be mathematically perfect, but the trend must clearly reflect the true archetype.

4. PredictedMove
Purpose

PredictedMove represents one ranked candidate for what the opponent is likely to play next.

This type exists because prediction should not return only one move.

The system should support ranking and comparing multiple likely moves.

Recommended Structure
final case class PredictedMove(
  move: Move,
  score: Double,
  probability: Double
)
Field Semantics
move: Move

The candidate move being predicted.

score: Double

Internal prediction score before or alongside probability normalization.

This is useful for debugging and ranking.

probability: Double

A display-friendly normalized estimate of likelihood.

This does not need to represent mathematically exact probability. It can be heuristic and relative, as long as:

higher means more likely,
values are consistently normalized,
ranking remains stable.
Important Design Rule

Prediction must rank multiple legal moves, not just pick one exact answer.

This is especially important for TUI integration, because highlighting top 3 likely destination squares is often more useful than pretending one exact move can always be known.

5. HighlightSquare
Purpose

HighlightSquare is the UI bridge type used by the TUI to render likely opponent target squares.

The TUI should not need to understand style inference, profile updates or move scoring.

It should only receive a simple representation of:

which squares to highlight,
how strongly,
optionally how to label them.
Recommended Structure
final case class HighlightSquare(
  position: Position,
  intensity: Double,
  label: String
)
Field Semantics
position: Position

The square to highlight.

intensity: Double

Relative highlight strength, usually in the range 0.0 to 1.0.

Example interpretation:

1.0 = strongest highlight
0.7 = second strongest
0.4 = weaker alternative
label: String

Optional annotation such as:

"1"
"2"
"3"
"!"
"Top"

This gives the TUI flexibility without requiring hard-coded assumptions.

Why This Type Exists

Without HighlightSquare, the TUI would need to inspect predicted moves directly.

That would unnecessarily couple UI code to prediction internals.

HighlightSquare keeps the UI integration small and clean.

6. PredictionResult
Purpose

PredictionResult is the output of the opponent move predictor.

It combines:

ranked predicted moves,
TUI-ready highlighted squares,
prediction-level confidence.

It answers the question:

“Given the current opponent profile and the current board state, what is the opponent most likely to do next?”

Recommended Structure
final case class PredictionResult(
  predictions: Vector[PredictedMove],
  highlights: Vector[HighlightSquare],
  confidence: Double
)
Field Semantics
predictions: Vector[PredictedMove]

A ranked list of likely legal opponent moves, ordered from most likely to least likely.

This should allow:

evaluation of top-1 accuracy,
evaluation of top-3 accuracy,
debugging of why one move was preferred over another.
highlights: Vector[HighlightSquare]

A TUI-ready collection of squares that should be highlighted on the board.

Usually these will be derived from the top predicted destination squares.

confidence: Double

Confidence in the prediction itself, not merely in the long-term style estimate.

This may depend on:

style confidence,
how strongly separated the top predicted move is from others,
how coherent the current board offers are.
Important Distinction

PredictionResult.confidence is not necessarily the same as OpponentProfile.confidence.

Example:

the style model may be confident,
but in a highly tactical position several moves may look similarly plausible,
so prediction confidence may be lower.

This distinction should be preserved.

# Required Flow Between Types

The architecture should support the following flow:

Step 1: Observe opponent move

An opponent move is made.

Step 2: Extract MoveFeatures

The system extracts a MoveFeatures instance from the move and the relevant board state.

Step 3: Update OpponentProfile

The profile is updated using the newly extracted features.

Step 4: Derive StyleEstimate

A display-friendly style estimate is generated from the updated profile.

Step 5: Predict opponent’s next moves

Using the updated profile and the current board state, the predictor ranks legal future opponent moves and returns a PredictionResult.

Step 6: Render highlights in the TUI

The TUI receives PredictionResult.highlights and renders them on the board.

This flow should remain explicit and modular.

TUI Highlight Integration Requirements
Goal

The TUI should visibly communicate where the opponent is most likely to move next.

This is a major proof-of-concept benefit because it makes opponent modeling:

visible,
debuggable,
demonstrable,
testable by eye.
Rendering Expectations

The TUI should be able to highlight at least the top predicted destination squares.

Recommended behavior:

strongest highlight for most likely square,
weaker highlights for second and third likely squares,
optional labels "1", "2", "3".

In this project, the rendering style is defined as:

- show the labels as numeric characters on the board squares (`1`, `2`, `3`, ...), where `1` is the most likely square.

The exact rendering style is implementation-specific, but the data interface must support this behavior.

TUI Decoupling Rule

The TUI must not:

compute the opponent profile,
infer style scores,
rank opponent moves.

It should only render HighlightSquare data that has already been prepared elsewhere.

This preserves clean separation of concerns.

Recommended Supporting Components

The following components are strongly recommended, although the exact names may vary.

OpponentMoveAnalyzer

Responsible for converting a move and game state into MoveFeatures.

OpponentProfileUpdater

Responsible for updating OpponentProfile from MoveFeatures.

StyleEstimator

Responsible for converting OpponentProfile into StyleEstimate.

OpponentMovePredictor

Responsible for scoring and ranking legal future moves and returning PredictionResult.

HighlightMapper

Optional helper for converting predicted moves into highlight squares.

This component split is recommended because it avoids one monolithic class doing everything.

Immutability Requirement

All of the above data types must be implemented as immutable case classes.

This is important because the project already benefits from functional design and testability.

Therefore:

no mutable fields,
no global mutable state,
no hidden caches inside the model objects.

All updates should return new instances.

Extensibility Requirements

This design must remain usable when more bots are added later.

Therefore the data model must allow adding new style dimensions in the future without breaking the entire architecture.

Examples of future expansions:

positional play tendency
simplification tendency
sacrificial tendency
endgame simplification preference
mobility preference
center-control preference

The current design should remain flexible enough for this.

Testing Expectations

The data model and pipeline should support tests for at least the following scenarios.

1. Profile update tests

A move that gives check and increases king pressure should increase aggression evidence.

2. Greed update tests

A move that captures a high-value piece should increase greed evidence.

3. Defensive update tests

A move that rescues an attacked own piece should increase defensive evidence.

4. Confidence tests

Confidence should increase as more consistent evidence is gathered.

5. Style estimate tests

A strongly aggressive profile should derive a style estimate with dominant aggressive percentage.

6. Prediction result tests

Predicted moves should be ranked consistently.

7. Highlight mapping tests

The top predicted destination squares should appear in the highlight list with descending intensity.

8. Proof-of-concept bot tests

A pure AggressiveBot observed over several moves should converge toward a strongly aggressive StyleEstimate.

Equivalent tests should be possible for:

GreedyBot
DefensiveBot
RandomBot

For RandomBot, the model should not falsely converge to an overly confident coherent style.

File / Package Recommendations

A possible structure could look like this:

makarchess/
  opponentmodel/
    MoveFeatures.scala
    OpponentProfile.scala
    StyleEstimate.scala
    PredictedMove.scala
    HighlightSquare.scala
    PredictionResult.scala
    OpponentMoveAnalyzer.scala
    OpponentProfileUpdater.scala
    StyleEstimator.scala
    OpponentMovePredictor.scala
    HighlightMapper.scala

And the runtime integration observer:

makarchess/
  opponentmodel/
    OpponentModelObserver.scala

The exact package path may vary depending on your project structure, but the subsystem should remain cohesive and separate from controller/view code.

Non-Goals

The following are explicitly not part of this specification:

exact machine-learning classification,
exact identification of concrete bot implementation,
strategic adaptation of the player’s own bot,
persistent training between sessions,
GUI rendering rules,
animation,
networked or distributed opponent modeling,
changing chess rules,
replacing existing bot architecture.

This feature is only about establishing a clean and extensible opponent-modeling data model and prediction output pipeline.

Acceptance Criteria

This task is considered complete when:

The project contains immutable case classes for:
MoveFeatures
OpponentProfile
StyleEstimate
PredictedMove
HighlightSquare
PredictionResult
OpponentProfile is clearly defined as internal rolling style evidence, not UI display state.
StyleEstimate is clearly defined as display-ready derived interpretation.
PredictionResult is clearly defined as the prediction output containing:
ranked moves,
highlight data,
prediction confidence.
The TUI integration contract is cleanly separated via HighlightSquare.

The data flow from:

observed move
feature extraction
profile update
style estimate
move prediction
TUI highlight rendering

is explicit and architecturally consistent.

The design remains extensible for future bots and future style dimensions.
The proof-of-concept case of detecting a pure archetypal opponent style is supported by the design.
The entire subsystem is structured in a way that is testable and does not require global mutable state.
Summary

This specification establishes a layered, extensible and testable foundation for opponent modeling.

The central design decision is:

model style tendencies, not hard-coded exact bot identity,
while still supporting proof-of-concept recognition of pure archetypal bots.

The required data model is:

MoveFeatures
OpponentProfile
StyleEstimate
PredictedMove
HighlightSquare
PredictionResult

These types should form a clean pipeline from observed behavior to visible next-move prediction in the TUI.