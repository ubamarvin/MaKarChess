package makarchess.model

enum Color:
  case White, Black

enum PieceType:
  case King, Queen, Rook, Bishop, Knight, Pawn

case class Piece(color: Color, kind: PieceType)

case class Position(file: Char, rank: Int):
  def fileIndex: Int = file - 'a'
  def isValid: Boolean =
    file >= 'a' && file <= 'h' && rank >= 1 && rank <= 8

object Position:
  def from(file: Char, rank: Int): Either[String, Position] =
    val f = file.toLower
    if f >= 'a' && f <= 'h' && rank >= 1 && rank <= 8 then Right(Position(f, rank))
    else Left("invalid position")

case class CastlingRights(
    whiteKingside: Boolean,
    whiteQueenside: Boolean,
    blackKingside: Boolean,
    blackQueenside: Boolean
):
  def without(color: Color): CastlingRights =
    color match
      case Color.White => copy(whiteKingside = false, whiteQueenside = false)
      case Color.Black => copy(blackKingside = false, blackQueenside = false)

object CastlingRights:
  val initial: CastlingRights =
    CastlingRights(true, true, true, true)

case class Move(from: Position, to: Position, promotion: Option[PieceType] = None)

case class PositionKey(
    board: Map[Position, Piece],
    sideToMove: Color,
    castling: CastlingRights,
    enPassant: Option[Position]
)

enum GamePhase:
  case InProgress
  case Checkmate(winner: Color)
  case Stalemate
  case DrawFiftyMoveRule
  case DrawThreefoldRepetition
  case DrawInsufficientMaterial

/** Full chess state used by the model (single source of truth). */
case class ChessState(
    board: Map[Position, Piece],
    sideToMove: Color,
    castling: CastlingRights,
    enPassant: Option[Position],
    halfmoveClock: Int,
    fullmoveNumber: Int,
    repetitionHistory: List[PositionKey],
    phase: GamePhase
):
  def positionKey: PositionKey =
    PositionKey(board, sideToMove, castling, enPassant)

enum MoveAttemptError:
  case InvalidInput
  case IllegalMove
  case GameAlreadyOver
