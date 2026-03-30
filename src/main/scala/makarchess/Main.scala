package makarchess

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, Color}
import makarchess.util.bot.{BotCaller, RandomBot}
import makarchess.view.{ConsoleIO, StdConsoleIO, TuiView}

object Main:

  def main(args: Array[String]): Unit =
    val botColor =
      args.toList.collectFirst {
        case "--bot=white" => Color.White
        case "--bot=black" => Color.Black
      }
    run(ChessModel(), StdConsoleIO(), botColor)

  def run(model: ChessModel, io: ConsoleIO, botPlays: Option[Color] = None): Unit =
    val controller = ChessController(model)
    val tui = TuiView(controller, io)

    botPlays.foreach { color =>
      val bot = RandomBot()
      val _ = BotCaller(controller, bot, color)
      ()
    }

    model.notifyObservers
    tui.runInteractive()
