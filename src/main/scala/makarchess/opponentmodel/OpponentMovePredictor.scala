package makarchess.opponentmodel

import makarchess.model.{ChessRules, ChessState}

object OpponentMovePredictor:

  def predict(profile: OpponentProfile, state: ChessState, max: Int = 3): PredictionResult =
    val legal = ChessRules.legalMoves(state).toVector
    if legal.isEmpty then PredictionResult(Vector.empty, Vector.empty, 0.0)
    else
      val sum = profile.aggressionEvidence + profile.greedEvidence + profile.defensiveEvidence
      val denom = if sum <= 1e-9 then 1.0 else sum

      val wAgg = 0.5 + (profile.aggressionEvidence / denom)
      val wGreed = 0.5 + (profile.greedEvidence / denom)
      val wDef = 0.5 + (profile.defensiveEvidence / denom)
      val wRisk = 0.25 + (profile.riskEvidence / (profile.observedMoves.toDouble + 1.0))

      def score(move: makarchess.model.Move): Double =
        val f = OpponentMoveAnalyzer.analyze(state, move)
        val sAgg = (if f.givesCheck then 2.0 else 0.0) + (if f.increasesKingPressure then 1.0 else 0.0)
        val sGreed = (if f.isCapture then 1.0 else 0.0) + (f.captureValue.toDouble / 3.0)
        val sDef = (if f.savesOwnPiece then 1.5 else 0.0) + (if !f.movesIntoDanger then 0.5 else 0.0)
        val sRisk = if f.movesIntoDanger then 1.0 else 0.0

        (wAgg * sAgg) + (wGreed * sGreed) + (wDef * sDef) + (wRisk * sRisk)

      val scored = legal.map(m => (m, score(m)))
      val maxScore = scored.map(_._2).max
      val exp = scored.map { case (m, s) => (m, s, math.exp(s - maxScore)) }
      val z = exp.map(_._3).sum

      val predicted =
        exp
          .map { case (m, s, e) => PredictedMove(move = m, score = s, probability = if z == 0 then 0.0 else e / z) }
          .sortBy(pm => -pm.probability)

      val highlights = HighlightMapper.fromPredictions(predicted, max)

      val topProb = predicted.headOption.map(_.probability).getOrElse(0.0)
      val secondProb = predicted.drop(1).headOption.map(_.probability).getOrElse(0.0)
      val separation = math.max(0.0, topProb - secondProb)
      val predictionConfidence = math.max(0.0, math.min(1.0, separation * (0.25 + profile.confidence)))

      PredictionResult(
        predictions = predicted,
        highlights = highlights,
        confidence = predictionConfidence
      )
