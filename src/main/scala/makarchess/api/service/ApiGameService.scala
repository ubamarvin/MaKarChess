package makarchess.api.service

import makarchess.api.dto.*
import makarchess.model.*
import makarchess.util.MoveResult

sealed trait ApiError:
  def message: String

object ApiError:
  final case class BadRequest(message: String) extends ApiError
  final case class Conflict(message: String) extends ApiError
  final case class Internal(message: String) extends ApiError

final class ApiGameService(registry: GameRegistry):
  import ApiError.*

  def newGame(userId: String, config: GameConfigRequest): Either[ApiError, GameStateResponse] =
    for
      botPlays <- parseOptionalColor(config.botPlays, "botPlays")
      modeledSide <- parseOptionalColor(config.modeledSide, "modeledSide")
      botType = config.botType.map(_.trim).filter(_.nonEmpty).getOrElse("random")
      controller <- registry.newGame(userId, GameRuntimeOptions(botType = botType, botPlays = botPlays, modeledSide = modeledSide)) match
        case Right(value) => Right(value)
        case Left(message) => Left(BadRequest(message))
    yield toGameStateResponse(controller.model.chessState)

  def resetGame(userId: String): Either[ApiError, GameStateResponse] =
    Right(toGameStateResponse(registry.resetGame(userId).model.chessState))

  def currentBoard(userId: String): Either[ApiError, GameStateResponse] =
    Right(toGameStateResponse(registry.currentController(userId).model.chessState))

  def currentStatus(userId: String): Either[ApiError, GameStatusResponse] =
    val controller = registry.currentController(userId)
    Right(toGameStatusResponse(controller.model.chessState, controller.snapshot))

  def replayStatus(userId: String): Either[ApiError, ReplayStatusResponse] =
    val controller = registry.currentController(userId)
    Right(
      ReplayStatusResponse(
        active = controller.hasActiveReplay,
        index = controller.replayIndex,
        length = controller.replayLength
      )
    )

  def loadFen(userId: String, request: FenRequest): Either[ApiError, GameStateResponse] =
    val trimmed = Option(request.fen).map(_.trim).getOrElse("")
    if trimmed.isEmpty then Left(BadRequest("FEN input is required."))
    else
      registry.currentController(userId).loadFenFromString(trimmed) match
        case Right(state) => Right(toGameStateResponse(state))
        case Left(message) => Left(BadRequest(message))

  def loadPgnReplay(userId: String, request: PgnRequest): Either[ApiError, GameStateResponse] =
    val trimmed = Option(request.pgn).map(_.trim).getOrElse("")
    if trimmed.isEmpty then Left(BadRequest("PGN input is required."))
    else
      registry.currentController(userId).loadReplayFromPgnString(trimmed) match
        case Right(state) => Right(toGameStateResponse(state))
        case Left(message) => Left(BadRequest(message))

  def replayForward(userId: String): Either[ApiError, GameStateResponse] =
    registry.currentController(userId).stepReplayForward() match
      case Right(state) => Right(toGameStateResponse(state))
      case Left(message) => Left(Conflict(message))

  def replayBackward(userId: String): Either[ApiError, GameStateResponse] =
    registry.currentController(userId).stepReplayBackward() match
      case Right(state) => Right(toGameStateResponse(state))
      case Left(message) => Left(Conflict(message))

  def makeMove(userId: String, uci: String): Either[ApiError, GameStateResponse] =
    val trimmed = Option(uci).map(_.trim).getOrElse("")
    if trimmed.isEmpty then Left(BadRequest("Move input is required."))
    else
      registry.currentController(userId).handleMoveInput(trimmed) match
        case MoveResult.Ok(()) => Right(toGameStateResponse(registry.currentController(userId).model.chessState))
        case MoveResult.Err(MoveAttemptError.InvalidInput) => Left(BadRequest(ChessModel.formatError(MoveAttemptError.InvalidInput)))
        case MoveResult.Err(MoveAttemptError.IllegalMove) => Left(Conflict(ChessModel.formatError(MoveAttemptError.IllegalMove)))
        case MoveResult.Err(MoveAttemptError.GameAlreadyOver) => Left(Conflict(ChessModel.formatError(MoveAttemptError.GameAlreadyOver)))

  private def toGameStateResponse(state: ChessState): GameStateResponse =
    GameStateResponse(
      board = state.board.toList.sortBy { case (pos, _) => (pos.rank, pos.file) }.map { case (position, piece) =>
        OccupiedSquareResponse(toPositionResponse(position), toPieceResponse(piece))
      },
      sideToMove = colorName(state.sideToMove),
      phase = toGamePhaseResponse(state.phase),
      castling = CastlingRightsResponse(
        state.castling.whiteKingside,
        state.castling.whiteQueenside,
        state.castling.blackKingside,
        state.castling.blackQueenside
      ),
      enPassant = state.enPassant.map(toPositionResponse),
      halfmoveClock = state.halfmoveClock,
      fullmoveNumber = state.fullmoveNumber,
      isCheck = isCheck(state)
    )

  private def toGameStatusResponse(state: ChessState, snapshot: GameSnapshot): GameStatusResponse =
    GameStatusResponse(
      sideToMove = colorName(state.sideToMove),
      phase = toGamePhaseResponse(state.phase),
      statusLine = snapshot.statusLine,
      currentPlayerLine = snapshot.currentPlayerLine,
      isCheck = isCheck(state)
    )

  private def toPositionResponse(position: Position): PositionResponse =
    PositionResponse(position.file.toString, position.rank)

  private def toPieceResponse(piece: Piece): PieceResponse =
    PieceResponse(colorName(piece.color), pieceTypeName(piece.kind))

  private def toGamePhaseResponse(phase: GamePhase): GamePhaseResponse =
    phase match
      case GamePhase.InProgress               => GamePhaseResponse("InProgress")
      case GamePhase.Stalemate                => GamePhaseResponse("Stalemate")
      case GamePhase.DrawFiftyMoveRule        => GamePhaseResponse("DrawFiftyMoveRule")
      case GamePhase.DrawThreefoldRepetition  => GamePhaseResponse("DrawThreefoldRepetition")
      case GamePhase.DrawInsufficientMaterial => GamePhaseResponse("DrawInsufficientMaterial")
      case GamePhase.Checkmate(winner)        => GamePhaseResponse("Checkmate", Some(colorName(winner)))

  private def colorName(color: Color): String =
    color match
      case Color.White => "White"
      case Color.Black => "Black"

  private def pieceTypeName(pieceType: PieceType): String =
    pieceType match
      case PieceType.King   => "King"
      case PieceType.Queen  => "Queen"
      case PieceType.Rook   => "Rook"
      case PieceType.Bishop => "Bishop"
      case PieceType.Knight => "Knight"
      case PieceType.Pawn   => "Pawn"

  private def isCheck(state: ChessState): Boolean =
    state.phase match
      case GamePhase.InProgress => ChessRules.isInCheck(state.board, state.sideToMove, state.enPassant)
      case _                    => false

  private def parseOptionalColor(value: Option[String], fieldName: String): Either[ApiError, Option[Color]] =
    value match
      case None => Right(None)
      case Some(raw) =>
        raw.trim.toLowerCase match
          case "" => Right(None)
          case "white" => Right(Some(Color.White))
          case "black" => Right(Some(Color.Black))
          case other => Left(BadRequest(s"Invalid $fieldName value: $other. Use 'white' or 'black'."))
