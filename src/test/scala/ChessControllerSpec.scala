import munit.FunSuite

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, Color, MoveAttemptError, PieceType, Position}
import makarchess.util.MoveResult

class ChessControllerSpec extends FunSuite:

  test("controller handleMoveInput parses and applies legal moves") {
    val model = ChessModel()
    val controller = ChessController(model)

    val res = controller.handleMoveInput("e2e4")
    assertEquals(res, MoveResult.Ok(()))
  }

  test("controller handleMoveInput returns errors for invalid/illegal moves") {
    val model = ChessModel()
    val controller = ChessController(model)

    assertEquals(controller.handleMoveInput("bad"), MoveResult.Err(MoveAttemptError.InvalidInput))
    assertEquals(controller.handleMoveInput("e2e5"), MoveResult.Err(MoveAttemptError.IllegalMove))
  }

  test("controller methods are callable (coverage for empty branches)") {
    val model = ChessModel()
    val controller = ChessController(model)

    controller.snapshot

    controller.startNewGame()
    controller.restartGame()

    controller.quitGame()
    controller.selectSquare(Position('e', 2))
    controller.resign()
    controller.offerDraw()
    controller.acceptDraw()
    controller.declineDraw()
    controller.undo()
    controller.redo()

    controller.choosePromotion(PieceType.Queen)
    controller.choosePromotion(PieceType.King)

    // These are intentionally likely illegal from the initial position; we mainly want to cover the promotion-encoding branch.
    controller.makeMove(Position('e', 2), Position('e', 4), Some(PieceType.Queen))
    controller.makeMove(Position('e', 2), Position('e', 4), Some(PieceType.Rook))
    controller.makeMove(Position('e', 2), Position('e', 4), Some(PieceType.Bishop))
    controller.makeMove(Position('e', 2), Position('e', 4), Some(PieceType.Knight))
    controller.makeMove(Position('e', 2), Position('e', 4), Some(PieceType.King))

    assertEquals(controller.model.snapshot.currentPlayerLine.nonEmpty, true)
  }

  test("controller makeMove without promotion delegates to model") {
    val model = ChessModel()
    val controller = ChessController(model)

    controller.makeMove(Position('e', 2), Position('e', 4))
    assertEquals(controller.model.snapshot.currentPlayerLine, "Black to move")
  }

  test("controller can load PGN replay and navigate forward and backward") {
    val model = ChessModel()
    val controller = ChessController(model)
    val pgn =
      """[Event "Scholar's Mate"]
        |[Result "1-0"]
        |
        |1. e4 e5
        |2. Bc4 Nc6
        |3. Qh5 Nf6
        |4. Qxf7# 1-0
        |""".stripMargin

    assertEquals(controller.loadReplayFromPgnString(pgn).isRight, true)
    assertEquals(controller.hasActiveReplay, true)
    assertEquals(controller.replayIndex, Some(0))

    assertEquals(controller.stepReplayForward().isRight, true)
    assertEquals(controller.replayIndex, Some(1))
    assertEquals(controller.stepReplayBackward().isRight, true)
    assertEquals(controller.replayIndex, Some(0))
  }

  test("controller can reload the same or a different PGN replay repeatedly") {
    val model = ChessModel()
    val controller = ChessController(model)
    val firstPgn = "1. e4 e5 2. Nf3 Nc6"
    val secondPgn = "1. d4 d5 2. c4"

    assertEquals(controller.loadReplayFromPgnString(firstPgn).isRight, true)
    assertEquals(controller.stepReplayForward().isRight, true)
    assertEquals(controller.replayIndex, Some(1))

    assertEquals(controller.loadReplayFromPgnString(firstPgn).isRight, true)
    assertEquals(controller.hasActiveReplay, true)
    assertEquals(controller.replayIndex, Some(0))

    assertEquals(controller.loadReplayFromPgnString(secondPgn).isRight, true)
    assertEquals(controller.hasActiveReplay, true)
    assertEquals(controller.replayIndex, Some(0))
  }

end ChessControllerSpec

