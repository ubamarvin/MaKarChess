package makarchess.serialization

import makarchess.model.*
import upickle.default.*

final class UpickleGameStateJsonCodec extends GameStateJsonCodec:
  import UpickleGameStateJsonCodec.*

  override def encode(state: ChessState): Either[String, String] =
    try Right(write(toJson(state)))
    catch case e: Exception => Left(s"Could not encode ChessState to JSON: ${e.getMessage}")

  override def decode(json: String): Either[String, ChessState] =
    try fromJson(read[ujson.Value](json))
    catch case e: Exception => Left(s"Could not decode ChessState from JSON: ${e.getMessage}")

object UpickleGameStateJsonCodec:
  given ReadWriter[Color] = readwriter[String].bimap(
    {
      case Color.White => "White"
      case Color.Black => "Black"
    },
    {
      case "White" => Color.White
      case "Black" => Color.Black
      case other     => throw new IllegalArgumentException(s"Unknown color: $other")
    }
  )

  given ReadWriter[PieceType] = readwriter[String].bimap(
    {
      case PieceType.King   => "King"
      case PieceType.Queen  => "Queen"
      case PieceType.Rook   => "Rook"
      case PieceType.Bishop => "Bishop"
      case PieceType.Knight => "Knight"
      case PieceType.Pawn   => "Pawn"
    },
    {
      case "King"   => PieceType.King
      case "Queen"  => PieceType.Queen
      case "Rook"   => PieceType.Rook
      case "Bishop" => PieceType.Bishop
      case "Knight" => PieceType.Knight
      case "Pawn"   => PieceType.Pawn
      case other      => throw new IllegalArgumentException(s"Unknown piece type: $other")
    }
  )

  given ReadWriter[Position] = macroRW
  given ReadWriter[Piece] = macroRW
  given ReadWriter[CastlingRights] = macroRW
  given ReadWriter[Move] = macroRW
  given ReadWriter[PositionKey] = macroRW

  given ReadWriter[GamePhase] = readwriter[ujson.Value].bimap(
    {
      case GamePhase.InProgress               => ujson.Obj("tag" -> "InProgress")
      case GamePhase.Stalemate                => ujson.Obj("tag" -> "Stalemate")
      case GamePhase.DrawFiftyMoveRule        => ujson.Obj("tag" -> "DrawFiftyMoveRule")
      case GamePhase.DrawThreefoldRepetition  => ujson.Obj("tag" -> "DrawThreefoldRepetition")
      case GamePhase.DrawInsufficientMaterial => ujson.Obj("tag" -> "DrawInsufficientMaterial")
      case GamePhase.Checkmate(winner)        => ujson.Obj("tag" -> "Checkmate", "winner" -> writeJs(winner))
    },
    json =>
      json.obj.get("tag").map(_.str) match
        case Some("InProgress")               => GamePhase.InProgress
        case Some("Stalemate")                => GamePhase.Stalemate
        case Some("DrawFiftyMoveRule")        => GamePhase.DrawFiftyMoveRule
        case Some("DrawThreefoldRepetition")  => GamePhase.DrawThreefoldRepetition
        case Some("DrawInsufficientMaterial") => GamePhase.DrawInsufficientMaterial
        case Some("Checkmate")                => GamePhase.Checkmate(read[Color](json.obj("winner")))
        case other                             => throw new IllegalArgumentException(s"Unknown game phase tag: $other")
  )

  private def toJson(state: ChessState): ujson.Value =
    ujson.Obj(
      "board" -> ujson.Arr.from(state.board.toSeq.sortBy { case (pos, _) => (pos.rank, pos.file) }.map { case (pos, piece) =>
        ujson.Obj(
          "position" -> writeJs(pos),
          "piece" -> writeJs(piece)
        )
      }),
      "sideToMove" -> writeJs(state.sideToMove),
      "castling" -> writeJs(state.castling),
      "enPassant" -> state.enPassant.map(writeJs(_)).getOrElse(ujson.Null),
      "halfmoveClock" -> state.halfmoveClock,
      "fullmoveNumber" -> state.fullmoveNumber,
      "repetitionHistory" -> writeJs(state.repetitionHistory),
      "phase" -> writeJs(state.phase)
    )

  private def fromJson(json: ujson.Value): Either[String, ChessState] =
    val obj = json.obj
    try
      Right(
        ChessState(
          board = obj("board").arr.toSeq.map { entry =>
            val entryObj = entry.obj
            read[Position](entryObj("position")) -> read[Piece](entryObj("piece"))
          }.toMap,
          sideToMove = read[Color](obj("sideToMove")),
          castling = read[CastlingRights](obj("castling")),
          enPassant = obj.get("enPassant") match
            case Some(ujson.Null) | None => None
            case Some(value)             => Some(read[Position](value)),
          halfmoveClock = obj("halfmoveClock").num.toInt,
          fullmoveNumber = obj("fullmoveNumber").num.toInt,
          repetitionHistory = read[List[PositionKey]](obj("repetitionHistory")),
          phase = read[GamePhase](obj("phase"))
        )
      )
    catch case e: Exception => Left(e.getMessage)
