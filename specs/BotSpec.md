Spec: Implement Random Bot for Scala Chess Project
Goal

Implement a first playable chess bot for the existing Scala chess project.

The architecture of the project is Model–View–Controller with Observer pattern.

This first bot version does not need strategy or evaluation.
It should simply choose one random legal move from the current game state.

Context

The chess game already contains:

a working model
a controller
rules / helper functions for:
generating legal moves
validating moves
applying moves

The bot must integrate into the current architecture cleanly.

Desired architecture
1. Bot

The Bot itself should be a separate component in its own folder/package, not part of model or controller.

The Bot is conceptually a pure decision unit:

it receives the current game state (preferred) or board representation
it calculates one move
it relies on already existing rule functions to ensure the move is legal
for now it chooses randomly among legal moves

Important:
Even if the board can be passed around easily, prefer passing the full game state if that is what the current rules need for legality checks.

2. Bot trait

Define a trait/interface for bots, so stronger bots can be added later.

Example intention:

every bot can calculate a move from the current state
bot returns Option[Move]
Some(move) if a legal move exists
None if no legal move exists (e.g. mate/stalemate)
3. BotCaller

Above the Bot sits a BotCaller.

The BotCaller is responsible for orchestration, not move calculation.

The BotCaller should:

be implemented as an Observer
subscribe to the model / observable update mechanism just like the view/TUI
react when the model changes
determine whether it is currently the bot’s turn
if yes, ask the Bot for a move
execute that move through the proper existing application flow

The Bot itself should not self-activate directly.
The BotCaller is the active observer/orchestrator.

Behavior
Random bot behavior

The first bot should:

receive current game state
ask existing chess rules for legal moves
choose one move randomly
return that move

It should not:

contain strategy
mutate the model directly
bypass existing validation/application logic
Turn handling

The Bot should only act when it is actually its turn.

This should be triggered through the BotCaller observer, which is notified after model updates.

Legality

The Bot must use the already existing move/rules infrastructure.

Do not reimplement chess legality from scratch.

The implementation should reuse existing functions for:

legal move generation
legality validation
state update / move execution
Integration requirements

Please implement:

New package/folder

Create a dedicated bot package/folder, for example:

bot/
  Bot.scala
  RandomBot.scala
  BotCaller.scala

Adjust naming/package structure to fit the existing project.

Bot trait

Add a bot abstraction trait.

RandomBot

Add a first concrete implementation that selects a random legal move.

BotCaller

Add a component that:

implements Observer
is registered to the observable/model update mechanism
reacts to state changes
invokes the bot only when the bot side is to move
Controller/model integration

Integrate the bot cleanly into the current game flow.

The existing human move flow should continue to work.

The bot move should be triggered automatically after the human move if it becomes the bot’s turn.

Play mode

Make it possible to play:

Human vs Bot

Optional but welcome if easy:

configure whether bot plays White or Black
Constraints
Keep the implementation simple
Reuse existing functions and rules
Do not redesign the whole architecture
Do not add advanced AI/search/minimax yet
Keep side effects localized
Preserve current MVC + Observer structure
Acceptance criteria

The implementation is complete when all of the following are true:

A Bot trait/interface exists
A RandomBot implementation exists
A BotCaller exists and acts as an observer
The bot is notified indirectly through observer-based model updates
When it is the bot’s turn, the bot makes exactly one legal move
The move is executed through the existing game flow
A human can play a full game against the bot
The bot never performs illegal moves
Existing human move functionality still works
Notes for implementation
Prefer passing full game state instead of only board if legality depends on more than board layout
Avoid recursive chaos or repeated triggering loops in observer handling
Ensure the bot does not make multiple moves for one turn
Keep naming and structure consistent with the current Scala project style
Deliverable

Provide the implementation plus a short summary of:

which files were added/changed
how the BotCaller is wired into the observer flow
how to start a game against the bot

Here is an even shorter version if you want to paste a tighter prompt into Cursor:

Implement a first chess bot for my Scala MVC + Observer chess project.

Requirements:

Add a separate Bot trait in its own bot package/folder
Add RandomBot that chooses one random move from the existing legal moves
Reuse existing rule/helper functions for legality and move application
Do not implement strategy/minimax yet
Add a BotCaller component above the bot
BotCaller must implement Observer
BotCaller must subscribe to model updates just like the TUI/view
When model updates, BotCaller checks whose turn it is
If it is the bot’s turn, it asks the bot for a move and executes it through the existing flow
Bot itself is just a move-calculation component, not the orchestrator
Prefer passing full game state instead of only board if current rule functions need it
Keep the architecture clean and close to the existing codebase

Acceptance criteria:

I can play the chess game vs the bot
The bot makes random legal moves only
Human move flow still works
Bot makes exactly one move when it is its turn

Also give me a short implementation summary listing changed files and how to run human vs bot.