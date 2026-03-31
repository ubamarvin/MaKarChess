package makarchess.opponentmodel

import makarchess.model.{ChessState, Color, Move}
import makarchess.util.bot.BotHeuristics

object OpponentMoveAnalyzer:

  def analyze(stateBefore: ChessState, move: Move): MoveFeatures =
    val mover = stateBefore.sideToMove
    val opponent = BotHeuristics.opposite(mover)

    val isCap = BotHeuristics.capturedPiece(stateBefore, move).nonEmpty
    val capVal = BotHeuristics.captureValue(stateBefore, move)
    val check = BotHeuristics.givesCheck(stateBefore, move)

    val next = BotHeuristics.nextState(stateBefore, move)

    val intoDanger = BotHeuristics.isSquareAttacked(next, move.to, by = next.sideToMove)

    val wasThreatened = BotHeuristics.isSquareAttacked(stateBefore, move.from, by = opponent)
    val nowThreatened = BotHeuristics.isSquareAttacked(next, move.to, by = next.sideToMove)
    val savesOwnPiece = wasThreatened && !nowThreatened

    val increasesKingPressure =
      val kingBefore = BotHeuristics.findKing(stateBefore.board, opponent)
      val kingAfter = BotHeuristics.findKing(next.board, opponent)
      (kingBefore, kingAfter) match
        case (Some(kb), Some(ka)) =>
          BotHeuristics.manhattanDistance(move.to, ka) < BotHeuristics.manhattanDistance(move.from, kb)
        case _ => false

    MoveFeatures(
      isCapture = isCap,
      captureValue = capVal,
      givesCheck = check,
      movesIntoDanger = intoDanger,
      savesOwnPiece = savesOwnPiece,
      increasesKingPressure = increasesKingPressure
    )
