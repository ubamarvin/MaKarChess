package makarchess.util.bot

import makarchess.model.{ChessRules, ChessState, Move}

final class GreedyBot extends Bot:

  override def chooseMove(state: ChessState): Option[Move] =
    val moves = ChessRules.legalMoves(state)
    if moves.isEmpty then None
    else
      Some(moves.maxBy(m => BotHeuristics.captureValue(state, m)))
