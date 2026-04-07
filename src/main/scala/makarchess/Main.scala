package makarchess

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, Color}
import makarchess.opponentmodel.OpponentModelObserver
import makarchess.util.bot.{BotCaller, BotFactory}
import makarchess.view.GuiLauncher
import makarchess.view.{ConsoleIO, StdConsoleIO, TuiView}

object Main:

  def main(args: Array[String]): Unit =
    val botColor =
      args.toList.collectFirst {
        case "--bot=white" => Color.White
        case "--bot=black" => Color.Black
      }

    val modeledSide =
      args.toList.collectFirst {
        case "--model=white" => Color.White
        case "--model=black" => Color.Black
      }

    val modeledMinMoves: Int =
      args.toList
        .collectFirst { case s if s.startsWith("--model-min-moves=") => s.drop("--model-min-moves=".length) }
        .flatMap(_.toIntOption)
        .filter(_ >= 0)
        .getOrElse(5)

    val botType =
      args.toList.collectFirst { case s if s.startsWith("--bot-type=") => s.drop("--bot-type=".length) }
        .getOrElse("random")

    val enableGui =
      args.toList.contains("--gui") ||
        sys.props.get("gui").exists(v => v.trim.equalsIgnoreCase("true") || v.trim == "1")

    run(ChessModel(), StdConsoleIO(), botColor, botType, modeledSide, modeledMinMoves, enableGui)

  def run(
      model: ChessModel,
      io: ConsoleIO,
      botPlays: Option[Color] = None,
      botType: String = "random",
      modeledSide: Option[Color] = None,
      modeledMinMoves: Int = 5,
      enableGui: Boolean = false
  ): Unit =
    val controller = ChessController(model)

    if enableGui then
      GuiLauncher.start(controller)

    modeledSide.foreach { side =>
      controller.setOpponentModelModeledSide(Some(side))
      val _ = OpponentModelObserver(controller, side, minObservedMovesForDisplay = modeledMinMoves)
      ()
    }

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
