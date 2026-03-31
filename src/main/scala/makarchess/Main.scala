package makarchess

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, Color}
import makarchess.util.bot.{BotCaller, BotFactory}
import makarchess.view.{ConsoleIO, StdConsoleIO, TuiView}

object Main:

  def main(args: Array[String]): Unit =
    val botColor =
      args.toList.collectFirst {
        case "--bot=white" => Color.White
        case "--bot=black" => Color.Black
      }

    val botType =
      args.toList.collectFirst { case s if s.startsWith("--bot-type=") => s.drop("--bot-type=".length) }
        .getOrElse("random")

    run(ChessModel(), StdConsoleIO(), botColor, botType)

  def run(
      model: ChessModel,
      io: ConsoleIO,
      botPlays: Option[Color] = None,
      botType: String = "random"
  ): Unit =
    val controller = ChessController(model)
    val tui = TuiView(controller, io)

    botPlays.foreach { color =>
      BotFactory.fromName(botType) match
        case Left(err) =>
          io.printLine(err)
        case Right(bot) =>
          val _ = BotCaller(controller, bot, color)
      ()
    }

    model.notifyObservers
    tui.runInteractive()
