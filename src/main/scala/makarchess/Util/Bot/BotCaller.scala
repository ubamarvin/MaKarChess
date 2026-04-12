package makarchess.util.bot

import makarchess.controller.ChessController
import makarchess.model.{Color, GamePhase}
import makarchess.util.Observer

final class BotCaller(
    controller: ChessController,
    bot: Bot,
    botPlays: Color
) extends Observer:

  controller.model.add(this)

  private var isExecuting: Boolean = false

  override def update: Unit =
    if isExecuting then ()
    else if controller.hasActiveReplay then ()
    else
      val state = controller.model.chessState
      if state.phase != GamePhase.InProgress then ()
      else if state.sideToMove != botPlays then ()
      else
        bot.chooseMove(state) match
          case None => ()
          case Some(move) =>
            isExecuting = true
            controller.makeMove(move.from, move.to, move.promotion)
            isExecuting = false

