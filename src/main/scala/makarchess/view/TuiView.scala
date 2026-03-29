package makarchess.view

import makarchess.controller.ChessController
import makarchess.model.ChessModel
import makarchess.util.Observer

trait ConsoleIO:
  def readLine(): Option[String]
  def printLine(line: String): Unit

class StdConsoleIO extends ConsoleIO:
  override def readLine(): Option[String] =
    Option(scala.io.StdIn.readLine())

  override def printLine(line: String): Unit =
    println(line)

/** TUI: observes the model via the shared observer contract; reads state through the controller. */
class TuiView(controller: ChessController, io: ConsoleIO) extends Observer:

  controller.model.add(this)

  override def update: Unit =
    val s = controller.snapshot
    io.printLine(s.boardText)
    if s.statusLine.nonEmpty then io.printLine(s.statusLine)
    if s.currentPlayerLine.nonEmpty then io.printLine(s.currentPlayerLine)

  /** Reads input, normalizes it, forwards actions to the controller (not the model directly). */
  def runInteractive(): Unit =
    @annotation.tailrec
    def loop(): Unit =
      io.printLine("Enter move (e.g. e2e4, e1g1, e7e8q) or 'quit':")
      io.readLine() match
        case None =>
          io.printLine("Goodbye.")
        case Some(raw) =>
          val input = raw.trim.toLowerCase
          if input == "quit" || input == "exit" then
            controller.quitGame()
            io.printLine("Goodbye.")
          else
            controller.handleMoveInput(input) match
              case Left(err) =>
                io.printLine(ChessModel.formatError(err))
                loop()
              case Right(()) =>
                loop()

    loop()
