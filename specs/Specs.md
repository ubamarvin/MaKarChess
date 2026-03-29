MVC with Observer means:

Model owns the data and business rules
View displays the data
Controller receives user input and translates it into actions on the model
Observer pattern lets views automatically react when the model changes
1. Roles
Model

The model contains:

board state
pieces
current player
game status
rule logic

The model should be the observable.

Why?
Because it is the part that changes, and others need to react to those changes.

View

The view shows the model state to the user.

Examples:

TUI
GUI

The view should be an observer.

Why?
Because it wants to be notified when the model changes.

A view usually does:

implement Observer
in update, re-read the current model state
redraw itself
Controller

The controller handles user actions.

Examples:

“move e2 to e4”
click on a square
start new game

The controller does not own the game state.
It calls methods on the model.

So the controller is usually neither observer nor observable in the basic setup.

2. Who implements what?

Typical setup:

Observer
trait Observer {
  def update: Unit
}

Implemented by:

Tui
later Gui
Observable
class Observable {
  var subscribers: Vector[Observer] = Vector()

  def add(s: Observer): Unit =
    subscribers = subscribers :+ s

  def remove(s: Observer): Unit =
    subscribers = subscribers.filterNot(_ == s)

  def notifyObservers: Unit =
    subscribers.foreach(_.update)
}

Extended or used by:

GameModel

For example:

class GameModel extends Observable

or

class GameModel {
  val observable = new Observable()
}

Usually extending is simpler.

3. Who is observer, who is observable?
Observable
Model
Observer
View
Tui
Gui
4. Data flow

The important direction is:

User -> Controller -> Model -> View

More concretely:

User enters a move in TUI or clicks in GUI
View forwards input to controller
Controller calls model method, for example:
model.makeMove(from, to)
Model validates and changes internal state
Model calls notifyObservers
All registered views receive update
Each view asks the model for the latest state
Each view redraws

So the data does not flow from view to view.

Instead:

one view triggers an action
the model changes
all views refresh from the same source of truth
5. Example flow

Suppose the user makes a move in the TUI.

Step-by-step
User types: e2 e4
Tui reads that input

Tui calls controller:

controller.handleMove("e2", "e4")

Controller parses and calls:

model.move(Position("e",2), Position("e",4))
Model:
checks if the move is legal
updates board
switches current player
updates status like check/checkmate

Model calls:

notifyObservers
Tui.update runs
Gui.update runs too, if a GUI exists
Both re-render using the current model state

So if TUI causes the move, GUI still updates automatically.

6. Important principle: views do not own truth

The view should never say:

“I think this piece moved”
“I store my own copy of the board”
“I decide whether it is checkmate”

Instead, the view should ask the model:

what does the board look like?
whose turn is it?
is the king in check?
is the game over?
what message should be shown?



Concrete example of mvc observer wiring:

main.scala
package de.htwg.se.Pokeymon

import de.htwg.se.Pokeymon.Controller.Controller
import de.htwg.se.Pokeymon.aView.Tui
import de.htwg.se.Pokeymon.Model.Game

object pokeymon {
  val game = new Game()
  val controller = new Controller(game) // why new?
  val tui = new Tui(controller)
  controller.notifyObservers // Warum

  @main
  def startGame(): Unit =
    var input: String = ""

    while (input != "q") {
      input = scala.io.StdIn.readLine()
      tui.processInput(input)
    }

}


Tui.scala
package de.htwg.se.Pokeymon.aView

import de.htwg.se.Pokeymon.Util.Observer
import de.htwg.se.Pokeymon.Controller.ControllerComponent.ControllerInterface

class Tui(controller: ControllerInterface) extends Observer {

  controller.add(this)

  def processInput(input: String): Unit = {
    input match {
      case "q" =>
        println("Game quitted")
      case "z" => controller.undo
      case "y" => controller.redo
      case "save" => controller.save
      case "load" => controller.load
      case _   => controller.handleInput(input)
    }
  }
  override def update: Unit = println(controller.printDisplay)
}

Controller.Scala
class Controller @Inject() (var game: GameInterface) extends Observable with ControllerInterface {
  private val commandManager = new CommandManager
  val injector = Guice.createInjector(new PokeymonModule)
  val fileIo = injector.instance[FileIOInterface]

  // UserInputHandler for all states
  def handleInput(input: String): Unit =
    // cmd.exe forwards input into model.game and model.game
    // pushes playersChoice on cmdManager stack
    commandManager.doStep(new HandleInputCommand(input, this))
    // game = game.handleInput(input)
    notifyObservers

  def undo: Unit =
    // cmd manager "injects" game with top stacks playersChoice
    // puts current one on the fuckin redo stack
    // println("undo in controller")
    commandManager.undoStep
    notifyObservers

  def redo: Unit =
    // println("redo in controller")
    // ...
    commandManager.redoStep
    notifyObservers

  def save: Unit =
    fileIo.save(game)
    println("SaveGame \n")
    notifyObservers

  def load: Unit =
    game = fileIo.load
    println("loadgame \n")
    notifyObservers

  def printDisplay: String =
    game.gameToString()

  def getSceneContent: Content =
    game.getContent()

}

