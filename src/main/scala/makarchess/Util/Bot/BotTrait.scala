package makarchess.util.bot

import makarchess.model.{ChessState, Move}

trait Bot:
  def chooseMove(state: ChessState): Option[Move]

