# Spec: Extend the Existing Bot System with GreedyBot, DefensiveBot and AggressiveBot

## Goal

The existing chess project should be extended with three additional bot types:

- `GreedyBot`
- `DefensiveBot`
- `AggressiveBot`

When starting the program, the user should be able to select via command line arguments:

- whether a bot is used
- which color the bot plays
- which bot type should be used

The extension must integrate into the existing architecture, specifically:

- keep the existing `Bot` interface
- continue using `BotCaller`
- use `ChessRules.legalMoves(state)` as the only source of legal moves
- continue using `ChessController.makeMove(...)` for actual move execution

---

## Existing Situation

The current system already provides a solid foundation:

- Bots implement a shared interface: `chooseMove(state: ChessState): Option[Move]`
- A `RandomBot` already exists as a reference implementation
- `BotCaller` is an observer, registers itself on the model and automatically performs the bot move when color and turn match
- `Main` currently optionally creates a bot depending on the selected color, but always uses `RandomBot`
- Game logic and legal moves are centralized in `ChessRules`

Therefore:

The extension should not redesign the controller or `BotCaller`, but only extend the bot system and startup configuration.

---

# Architecture Requirements

## 1. Keep the Existing Bot Interface Unchanged

The existing interface is mandatory and must not be changed:

```scala
trait Bot:
  def chooseMove(state: ChessState): Option[Move]

All new bots must implement this interface.

There should be no special-case handling inside BotCaller for individual bot types. BotCaller must continue to work exclusively with the Bot interface.

2. New Bot Classes

At minimum, the following new classes should be introduced:

final class GreedyBot extends Bot
final class DefensiveBot extends Bot
final class AggressiveBot extends Bot

Optionally, a shared scoring abstraction may be introduced, as long as only Bot remains visible externally.

Example of an allowed internal architecture:

trait Bot
trait MoveEvaluator
abstract class EvaluatingBot(...) extends Bot

GreedyBot, DefensiveBot and AggressiveBot would each use a different evaluation strategy.

The only important requirement is:

For the rest of the system, all three bots must appear as normal Bot implementations.

Functional Requirements
3. GreedyBot

GreedyBot should choose the legal move with the highest immediate material gain.

Expected behavior:

prefer captures with high material gain
prefer capturing a queen over a rook
prefer capturing a rook over a bishop or knight
prefer capturing a bishop or knight over a pawn
if no capture is available, the bot may use a simple fallback heuristic, for example:
first legal move
random legal move
move with a neutral score

Recommended material values:

Pawn   = 1
Knight = 3
Bishop = 3
Rook   = 5
Queen  = 9

The king should not be evaluated because legal moves never capture a king.

The bot may simulate the resulting board state for evaluation, but must exclusively work with legal moves from:

ChessRules.legalMoves(state)
4. AggressiveBot

AggressiveBot should prefer moves that apply direct pressure to the opponent.

Expected behavior:

prefer moves that give check
prefer captures
prefer active moves toward enemy pieces or the enemy king
prefer development and forward movement over retreat or purely passive moves

Recommended heuristics:

large bonus if the move puts the enemy king in check in the resulting state
bonus for captures
bonus if the moved piece attacks enemy pieces after the move
bonus if the move reduces the distance to the enemy king
small bonus for center control or forward movement

Important:

AggressiveBot is allowed to play risky moves. It does not need to prioritize material safety as long as the move is legal.

5. DefensiveBot

DefensiveBot should prefer moves that avoid losses and protect endangered own pieces.

Expected behavior:

move threatened pieces to safety
prefer safe or safer destination squares
prefer moves that defend own pieces
avoid moves where the moved piece can immediately be captured by a piece of equal or lower value
avoid unnecessary sacrifices

Recommended heuristics:

penalty if the destination square is attacked by enemy pieces in the resulting state
bonus if a currently threatened own piece is saved
bonus if the move increases protection of own pieces
bonus if the move blocks or weakens an enemy attack
small bonus for castling or king safety if appropriate

DefensiveBot should not merely play passively or randomly, but deliberately choose safer alternatives among the legal moves.

Technical Requirements
6. Legal Moves Must Come Exclusively from ChessRules

All bots must derive their candidate moves exclusively from:

ChessRules.legalMoves(state)

No bot may introduce its own move legality logic.

7. BotCaller Remains Unchanged or Nearly Unchanged

BotCaller is already correctly connected as an observer and calls:

bot.chooseMove(state)

It then executes the move through the controller.

This structure should remain unchanged.

Not desired:

bot performs model operations directly
bot writes directly into the board
bot bypasses the controller

Desired:

bot only selects a move
BotCaller delegates move execution to:
ChessController.makeMove(...)
8. Extend Bot Selection Through Command Line Arguments

Currently, Main only accepts the bot color and always creates a RandomBot.

This should be extended.

New CLI Requirements

Supported arguments:

--bot=white
--bot=black
--bot-type=random
--bot-type=greedy
--bot-type=defensive
--bot-type=aggressive

Examples:

sbt "run --bot=black --bot-type=greedy"
sbt "run --bot=white --bot-type=defensive"
sbt "run --bot=black --bot-type=aggressive"
sbt "run --bot=white --bot-type=random"

Behavior:

if --bot= is missing → play human vs human
if --bot= is provided but --bot-type= is missing → default to random
invalid bot types should be handled cleanly, e.g. error message and termination or fallback to random
invalid colors should also be handled cleanly
9. Introduce BotFactory

Bot creation should be moved out of Main.

There should be a central factory, for example:

object BotFactory:
  def fromName(name: String): Either[String, Bot]

Behavior:

"random"     -> RandomBot
"greedy"     -> GreedyBot
"defensive"  -> DefensiveBot
"aggressive" -> AggressiveBot
unknown name -> Left("Unknown bot type: ...")

Benefits:

Main remains clean
new bots can easily be added later
bot creation is centralized in one place
Quality Requirements
10. No Copy-Paste of Core Logic

It is not desired that all bots duplicate the same loop over legal moves.

Instead, shared reuse is desired, for example:

shared helper functions for material values
shared helper functions for resulting states
shared base for evaluation logic
shared sorting / maximization logic

Allowed example:

trait MoveScoringBot extends Bot

or

abstract class HeuristicBot extends Bot

with a bot-specific:

scoreMove(...)

method.

11. Purely Read-Only Bot Logic

Bots should only read the provided ChessState and derive a move from it.

Bots may:

read legal moves
simulate resulting states
calculate scores

Bots may not:

introduce global mutable state
directly know the controller
replace the model
register observers

These responsibilities remain outside the bot implementation.

This matches the existing design: Bot only knows ChessState and Move.

12. Testability

The new bots should be implemented in a way that makes them testable.

At minimum, the following cases should be testable:

GreedyBot chooses a capture with higher material value over one with lower material value
DefensiveBot prefers a safe move over an unsafe one in a simple position
AggressiveBot prefers a move giving check over a neutral move in a simple position
BotFactory returns the correct bot type for all supported names
argument parsing correctly maps --bot-type=...
Concrete Implementation Tasks
13. Files / Components

The existing bot structure should be extended approximately like this:

makarchess.util.bot/
  Bot.scala                // already exists
  RandomBot.scala          // already exists
  GreedyBot.scala
  DefensiveBot.scala
  AggressiveBot.scala
  BotFactory.scala
  BotHeuristics.scala      // optional

Additional helper classes may optionally be introduced if they simplify the architecture.

14. Changes to Main

Main should no longer directly instantiate RandomBot() all the time, but instead choose a bot type depending on the arguments.

Current behavior: RandomBot() is always used.

Desired behavior:

bot color comes from --bot=...
bot type comes from --bot-type=...
bot is created through BotFactory
continue using:
BotCaller(controller, bot, color)

unchanged.

Non-Goals

The following are NOT part of this extension:

Minimax
Alpha-Beta pruning
Opening book
Opponent modeling
Multiple bots in the same game
Changes to chess rules
GUI-specific bot configuration
Refactoring of ChessController or ChessModel except what is necessary for argument handling

The extension should intentionally remain small and architecturally clean.

Acceptance Criteria

The task is considered complete when:

GreedyBot, DefensiveBot and AggressiveBot exist and implement the existing Bot interface.
BotCaller continues to work with all bots without special-case logic.
Main can choose the bot type via CLI arguments instead of always using RandomBot.
All bots derive their moves exclusively from ChessRules.legalMoves(state).
The game still starts normally, moves remain executable, and bot moves are performed through the controller.
Invalid bot types produce clean error handling or a clearly defined fallback.
The implementation preserves the existing MVC and Observer architecture.