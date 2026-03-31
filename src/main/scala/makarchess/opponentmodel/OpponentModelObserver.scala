package makarchess.opponentmodel

import makarchess.controller.ChessController
import makarchess.model.{ChessRules, ChessState, Color, GamePhase, Move}
import makarchess.util.Observer
import makarchess.util.bot.BotHeuristics

final class OpponentModelObserver(
    controller: ChessController,
    modeledSide: Color,
    maxHighlights: Int = 3,
    minObservedMovesForDisplay: Int = 5
) extends Observer:

  controller.model.add(this)

  private var previous: Option[ChessState] = None
  private var profile: OpponentProfile = OpponentProfile.empty

  override def update: Unit =
    val current = controller.model.chessState

    if isRestart(previous, current) then
      profile = OpponentProfile.empty

      controller.setOpponentModelObservedMoves(0)
      controller.setOpponentModelStyleEstimate(None)
      controller.setOpponentModelPrediction(None)
      controller.setOpponentModelHighlights(Vector.empty)
      controller.setOpponentModelStatusMessage(None)

    previous.foreach { prev =>
      val justMoved = prev.sideToMove == modeledSide && current.sideToMove == BotHeuristics.opposite(modeledSide)
      if justMoved then
        detectMove(prev, current).foreach { move =>
          val features = OpponentMoveAnalyzer.analyze(prev, move)
          profile = OpponentProfileUpdater.update(profile, features)
        }
    }

    controller.setOpponentModelObservedMoves(profile.observedMoves)

    if current.phase != GamePhase.InProgress then
      controller.setOpponentModelStyleEstimate(None)
      controller.setOpponentModelPrediction(None)
      controller.setOpponentModelHighlights(Vector.empty)
      controller.setOpponentModelStatusMessage(None)
    else if profile.observedMoves < minObservedMovesForDisplay then
      controller.setOpponentModelStyleEstimate(None)
      controller.setOpponentModelPrediction(None)
      controller.setOpponentModelHighlights(Vector.empty)
      controller.setOpponentModelStatusMessage(
        Some(s"Opponent model gathering data... (${profile.observedMoves}/$minObservedMovesForDisplay)")
      )
    else
      controller.setOpponentModelStatusMessage(None)
      controller.setOpponentModelStyleEstimate(Some(StyleEstimator.fromProfile(profile)))
      val predictionState =
        if current.sideToMove == modeledSide then current
        else current.copy(sideToMove = modeledSide)

      val prediction = OpponentMovePredictor.predict(profile, predictionState, max = maxHighlights)
      controller.setOpponentModelPrediction(Some(prediction))
      controller.setOpponentModelHighlights(prediction.highlights)

    previous = Some(current)

  private def detectMove(before: ChessState, after: ChessState): Option[Move] =
    val candidates = ChessRules.legalMoves(before)
    candidates.find { m =>
      val next = ChessRules.applyLegalMove(before, m)
      next.positionKey == after.positionKey
    }

  private def isRestart(prev: Option[ChessState], current: ChessState): Boolean =
    prev match
      case None => false
      case Some(p) =>
        val init = ChessRules.initialState
        p.positionKey != current.positionKey && current.positionKey == init.positionKey
