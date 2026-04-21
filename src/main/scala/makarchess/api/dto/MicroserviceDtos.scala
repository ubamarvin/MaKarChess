package makarchess.api.dto

final case class BotTypesResponse(types: List[String])

final case class BotMoveRequest(botType: Option[String] = None, fen: Option[String] = None)

final case class BotMoveResponse(
    botType: String,
    uci: Option[String],
    from: Option[PositionResponse],
    to: Option[PositionResponse],
    promotion: Option[String],
    sideToMove: String,
    legalMoves: Int,
    reason: Option[String] = None
)

final case class AnalysisRequest(fen: Option[String] = None)

final case class MaterialResponse(white: Int, black: Int, balanceForWhite: Int)

final case class AnalysisResponse(
    sideToMove: String,
    phase: GamePhaseResponse,
    isCheck: Boolean,
    legalMoves: Int,
    material: MaterialResponse,
    bestCapture: Option[String],
    legalMoveUcis: List[String]
)

final case class RankingPlayerRequest(player: String)

final case class RankingResultRequest(whitePlayer: String, blackPlayer: String, result: String)

final case class RankingEntryResponse(
    player: String,
    rating: Int,
    wins: Int,
    draws: Int,
    losses: Int,
    games: Int
)

final case class RankingResponse(entries: List[RankingEntryResponse])

final case class RankingResultResponse(white: RankingEntryResponse, black: RankingEntryResponse)
