package makarchess.util.bot

import makarchess.model.{ChessRules, ChessState, Color, Move}

final class AggressiveBot extends Bot:

  override def chooseMove(state: ChessState): Option[Move] =
    val moves = ChessRules.legalMoves(state)
    if moves.isEmpty then None
    else
      val enemy = BotHeuristics.opposite(state.sideToMove)
      val enemyKingOpt = BotHeuristics.findKing(state.board, enemy)

      def score(move: Move): Int =
        val next = BotHeuristics.nextState(state, move)
        val checkBonus = if BotHeuristics.givesCheck(state, move) then 1000 else 0
        val captureBonus = BotHeuristics.captureValue(state, move) * 100

        val distBonus =
          enemyKingOpt match
            case None => 0
            case Some(k) =>
              val before = BotHeuristics.manhattanDistance(move.from, k)
              val after = BotHeuristics.manhattanDistance(move.to, k)
              (before - after) * 5

        val forwardBonus =
          state.sideToMove match
            case Color.White => move.to.rank - move.from.rank
            case Color.Black => move.from.rank - move.to.rank

        checkBonus + captureBonus + distBonus + forwardBonus

      Some(moves.maxBy(score))
