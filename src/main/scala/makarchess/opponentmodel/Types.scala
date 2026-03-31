package makarchess.opponentmodel

import makarchess.model.{Move, Position}

final case class MoveFeatures(
    isCapture: Boolean,
    captureValue: Int,
    givesCheck: Boolean,
    movesIntoDanger: Boolean,
    savesOwnPiece: Boolean,
    increasesKingPressure: Boolean
)

final case class OpponentProfile(
    observedMoves: Int,
    aggressionEvidence: Double,
    greedEvidence: Double,
    defensiveEvidence: Double,
    riskEvidence: Double,
    confidence: Double
)

object OpponentProfile:
  val empty: OpponentProfile =
    OpponentProfile(
      observedMoves = 0,
      aggressionEvidence = 0.0,
      greedEvidence = 0.0,
      defensiveEvidence = 0.0,
      riskEvidence = 0.0,
      confidence = 0.0
    )

final case class StyleEstimate(
    aggressivePct: Double,
    greedyPct: Double,
    defensivePct: Double,
    riskTolerancePct: Double,
    confidencePct: Double
)

final case class PredictedMove(
    move: Move,
    score: Double,
    probability: Double
)

final case class HighlightSquare(
    position: Position,
    intensity: Double,
    label: String
)

final case class PredictionResult(
    predictions: Vector[PredictedMove],
    highlights: Vector[HighlightSquare],
    confidence: Double
)
