package makarchess.api.service

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, Color}
import makarchess.opponentmodel.OpponentModelObserver
import makarchess.parser.ParserModule
import makarchess.parser.api.ParserBackend
import makarchess.persistence.{FenFileService, GameStateJsonService, LocalFileIO, PgnFileService}
import makarchess.serialization.UpickleGameStateJsonCodec
import makarchess.util.bot.{BotCaller, BotFactory}

final case class GameRuntimeOptions(
    botType: String = "random",
    botPlays: Option[Color] = None,
    modeledSide: Option[Color] = None
)

final class GameRegistry:
  private val fileIO = LocalFileIO()
  private val fenParser = ParserModule.fenParser(ParserBackend.Fast)
  private val pgnParser = ParserModule.pgnParser(ParserBackend.Fast)

  private def baseController(): ChessController =
    new ChessController(
      ChessModel(),
      fenParser,
      pgnParser,
      FenFileService(fileIO, fenParser),
      PgnFileService(fileIO, pgnParser),
      GameStateJsonService(fileIO, UpickleGameStateJsonCodec())
    )

  private def configuredController(options: GameRuntimeOptions): Either[String, ChessController] =
    val controller = baseController()

    options.modeledSide.foreach { side =>
      controller.setOpponentModelModeledSide(Some(side))
      val _ = OpponentModelObserver(controller, side, minObservedMovesForDisplay = 5)
      ()
    }

    options.botPlays match
      case None =>
        controller.model.notifyObservers
        Right(controller)
      case Some(color) =>
        BotFactory.fromName(options.botType).map { bot =>
          val _ = BotCaller(controller, bot, color)
          controller.model.notifyObservers
          controller
        }

  private var controller: ChessController = configuredController(GameRuntimeOptions()).fold(_ => baseController(), identity)

  def currentController: ChessController = controller

  def newGame(options: GameRuntimeOptions = GameRuntimeOptions()): Either[String, ChessController] =
    configuredController(options).map { nextController =>
      controller = nextController
      nextController
    }

  def resetGame(): ChessController =
    controller.restartGame()
    controller
