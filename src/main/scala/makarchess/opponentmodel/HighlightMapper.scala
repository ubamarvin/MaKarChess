package makarchess.opponentmodel

import makarchess.model.Position

object HighlightMapper:

  def fromPredictions(predictions: Vector[PredictedMove], max: Int): Vector[HighlightSquare] =
    val top = predictions.take(max)

    val intensities = Vector(1.0, 0.7, 0.4)

    top.zipWithIndex.map { case (pm, idx) =>
      HighlightSquare(
        position = pm.move.to,
        intensity = intensities.lift(idx).getOrElse(0.25),
        label = (idx + 1).toString
      )
    }
