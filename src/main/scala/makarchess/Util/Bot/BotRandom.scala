package makarchess.util.bot

import makarchess.model.{ChessRules, ChessState, Move}

import scala.util.Random

final class RandomBot(random: Random = Random() ) extends Bot:

  override def chooseMove(state: ChessState): Option[Move] =
    val moves = ChessRules.legalMoves(state)
    if moves.isEmpty then None
    else Some(moves(random.nextInt(moves.size)))

