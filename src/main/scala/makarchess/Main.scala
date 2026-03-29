package makarchess

import makarchess.controller.ChessController
import makarchess.model.ChessModel
import makarchess.view.{ConsoleIO, StdConsoleIO, TuiView}

object Main:

  def main(args: Array[String]): Unit =
    run(new ChessModel(), StdConsoleIO())

  def run(model: ChessModel, io: ConsoleIO): Unit =
    val controller = ChessController(model)
    val tui = TuiView(controller, io)
    model.notifyObservers
    tui.runInteractive()
