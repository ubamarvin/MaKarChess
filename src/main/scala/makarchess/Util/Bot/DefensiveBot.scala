package makarchess.util.bot

import makarchess.model.{ChessRules, ChessState, Move}

final class DefensiveBot extends Bot:

  override def chooseMove(state: ChessState): Option[Move] =
    val moves = ChessRules.legalMoves(state)
    if moves.isEmpty then None
    else
      val enemy = BotHeuristics.opposite(state.sideToMove)

      def score(move: Move): Int =
        val mover = state.board(move.from)
        val moverValue = BotHeuristics.pieceValue.getOrElse(mover.kind, 0)

        val threatenedBefore = BotHeuristics.isSquareAttacked(state, move.from, enemy)
        val next = BotHeuristics.nextState(state, move)
        val threatenedAfter = ChessRules.isSquareAttacked(next.board, move.to, enemy, next.enPassant)

        val saveBonus = if threatenedBefore && !threatenedAfter then 200 else 0
        val dangerPenalty = if threatenedAfter then moverValue * 120 else 0
        val captureBonus = BotHeuristics.captureValue(state, move) * 20

        saveBonus + captureBonus - dangerPenalty

      Some(moves.maxBy(score))
