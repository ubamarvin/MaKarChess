import munit.FunSuite

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, ChessRules, Color}
import makarchess.util.bot.{BotCaller, RandomBot}
import makarchess.util.MoveResult

import scala.util.Random

class BotSpec extends FunSuite:

  test("bot makes exactly one legal move when it becomes its turn") {
    val model = ChessModel()
    val controller = ChessController(model)

    val bot = RandomBot(Random(0))
    val _ = BotCaller(controller, bot, Color.Black)

    val before = controller.model.chessState
    assertEquals(before.sideToMove, Color.White)

    val res = controller.handleMoveInput("e2e4")
    assertEquals(res, MoveResult.Ok(()))

    // After white moves, controller notifies observers, BotCaller should respond once for black.
    val after = controller.model.chessState
    assertEquals(after.sideToMove, Color.White)

    // Ensure exactly one fullmove was completed (white+black): fullmoveNumber increments after black.
    assertEquals(after.fullmoveNumber, before.fullmoveNumber + 1)

    // Ensure the bot's move was legal from the post-human-move position.
    val stateAfterHuman =
      ChessRules.parseUci("e2e4") match
        case MoveResult.Err(err) => fail(s"Unexpected parse error: ${ChessModel.formatError(err)}")
        case MoveResult.Ok(mv) => ChessRules.applyLegalMove(before, mv)

    val legalReplies = ChessRules.legalMoves(stateAfterHuman)
    assert(legalReplies.nonEmpty)

    val possibleNextStates = legalReplies.map(r => ChessRules.applyLegalMove(stateAfterHuman, r))
    assert(possibleNextStates.contains(after))
  }

  test("bot stays idle while a PGN replay is active") {
    val model = ChessModel()
    val controller = ChessController(model)

    val bot = RandomBot(Random(0))
    val _ = BotCaller(controller, bot, Color.Black)

    val pgn =
      """[Event "Scholar's Mate"]
        |[Result "1-0"]
        |
        |1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6 4.Qxf7# 1-0
        |""".stripMargin

    assertEquals(controller.loadReplayFromPgnString(pgn).isRight, true)
    assertEquals(controller.hasActiveReplay, true)
    assertEquals(controller.replayIndex, Some(0))

    assertEquals(controller.stepReplayForward().isRight, true)
    assertEquals(controller.hasActiveReplay, true)
    assertEquals(controller.replayIndex, Some(1))
  }
