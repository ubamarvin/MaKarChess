package makarchess.opponentmodel

object OpponentProfileUpdater:

  def update(profile: OpponentProfile, features: MoveFeatures): OpponentProfile =
    val aInc = (if features.givesCheck then 2.0 else 0.0) + (if features.increasesKingPressure then 1.0 else 0.0)
    val gInc = (if features.isCapture then 1.0 else 0.0) + (features.captureValue.toDouble / 3.0)
    val dInc = (if features.savesOwnPiece then 2.0 else 0.0) + (if !features.movesIntoDanger then 0.5 else 0.0)
    val rInc = if features.movesIntoDanger then 1.0 else 0.0

    val next =
      profile.copy(
        observedMoves = profile.observedMoves + 1,
        aggressionEvidence = profile.aggressionEvidence + aInc,
        greedEvidence = profile.greedEvidence + gInc,
        defensiveEvidence = profile.defensiveEvidence + dInc,
        riskEvidence = profile.riskEvidence + rInc
      )

    val sum = next.aggressionEvidence + next.greedEvidence + next.defensiveEvidence
    val maxDim = math.max(next.aggressionEvidence, math.max(next.greedEvidence, next.defensiveEvidence))
    val consistency = if sum <= 1e-9 then 0.0 else maxDim / sum

    val byMoves = math.min(1.0, next.observedMoves.toDouble / 12.0)
    val conf = byMoves * consistency

    next.copy(confidence = conf)
