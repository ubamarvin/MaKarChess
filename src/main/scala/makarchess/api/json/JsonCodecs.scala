package makarchess.api.json

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import makarchess.api.dto.*

object JsonCodecs:
  given Codec[GameConfigRequest] = deriveCodec
  given Codec[FenRequest] = deriveCodec
  given Codec[PgnRequest] = deriveCodec
  given Codec[MoveRequest] = deriveCodec
  given Codec[ErrorResponse] = deriveCodec
  given Codec[HealthResponse] = deriveCodec
  given Codec[PositionResponse] = deriveCodec
  given Codec[PieceResponse] = deriveCodec
  given Codec[OccupiedSquareResponse] = deriveCodec
  given Codec[CastlingRightsResponse] = deriveCodec
  given Codec[GamePhaseResponse] = deriveCodec
  given Codec[GameStateResponse] = deriveCodec
  given Codec[GameStatusResponse] = deriveCodec
  given Codec[ReplayStatusResponse] = deriveCodec
  given Codec[BotTypesResponse] = deriveCodec
  given Codec[BotMoveRequest] = deriveCodec
  given Codec[BotMoveResponse] = deriveCodec
  given Codec[AnalysisRequest] = deriveCodec
  given Codec[MaterialResponse] = deriveCodec
  given Codec[AnalysisResponse] = deriveCodec
  given Codec[RankingPlayerRequest] = deriveCodec
  given Codec[RankingResultRequest] = deriveCodec
  given Codec[RankingEntryResponse] = deriveCodec
  given Codec[RankingResponse] = deriveCodec
  given Codec[RankingResultResponse] = deriveCodec
