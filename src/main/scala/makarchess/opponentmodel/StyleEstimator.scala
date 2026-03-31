package makarchess.opponentmodel

object StyleEstimator:

  private def clampPct(x: Double): Double =
    math.max(0.0, math.min(100.0, x))

  def fromProfile(profile: OpponentProfile): StyleEstimate =
    val sum = profile.aggressionEvidence + profile.greedEvidence + profile.defensiveEvidence
    val denom = if sum <= 1e-9 then 1.0 else sum

    val aggressive = 100.0 * (profile.aggressionEvidence / denom)
    val greedy = 100.0 * (profile.greedEvidence / denom)
    val defensive = 100.0 * (profile.defensiveEvidence / denom)

    val riskTol =
      val perMove = if profile.observedMoves <= 0 then 0.0 else profile.riskEvidence / profile.observedMoves.toDouble
      100.0 * math.max(0.0, math.min(1.0, perMove))

    StyleEstimate(
      aggressivePct = clampPct(aggressive),
      greedyPct = clampPct(greedy),
      defensivePct = clampPct(defensive),
      riskTolerancePct = clampPct(riskTol),
      confidencePct = clampPct(100.0 * profile.confidence)
    )
