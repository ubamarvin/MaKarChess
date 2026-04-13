package makarchess.api.dto

final case class PositionResponse(file: String, rank: Int)

final case class PieceResponse(color: String, kind: String)

final case class OccupiedSquareResponse(position: PositionResponse, piece: PieceResponse)

final case class CastlingRightsResponse(
    whiteKingside: Boolean,
    whiteQueenside: Boolean,
    blackKingside: Boolean,
    blackQueenside: Boolean
)

final case class GamePhaseResponse(tag: String, winner: Option[String] = None)

final case class GameStateResponse(
    board: List[OccupiedSquareResponse],
    sideToMove: String,
    phase: GamePhaseResponse,
    castling: CastlingRightsResponse,
    enPassant: Option[PositionResponse],
    halfmoveClock: Int,
    fullmoveNumber: Int,
    isCheck: Boolean
)

final case class GameStatusResponse(
    sideToMove: String,
    phase: GamePhaseResponse,
    statusLine: String,
    currentPlayerLine: String,
    isCheck: Boolean
)
