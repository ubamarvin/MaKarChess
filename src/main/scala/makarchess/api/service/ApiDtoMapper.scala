package makarchess.api.service

import makarchess.api.dto.*
import makarchess.model.*

object ApiDtoMapper:

  def toGameStateResponse(state: ChessState): GameStateResponse =
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

  def toPositionResponse(position: Position): PositionResponse =
    PositionResponse(position.file.toString, position.rank)

  def toPieceResponse(piece: Piece): PieceResponse =
    PieceResponse(colorName(piece.color), pieceTypeName(piece.kind))

  def toGamePhaseResponse(phase: GamePhase): GamePhaseResponse =
    phase match
      case GamePhase.InProgress               => GamePhaseResponse("InProgress")
      case GamePhase.Stalemate                => GamePhaseResponse("Stalemate")
      case GamePhase.DrawFiftyMoveRule        => GamePhaseResponse("DrawFiftyMoveRule")
      case GamePhase.DrawThreefoldRepetition  => GamePhaseResponse("DrawThreefoldRepetition")
      case GamePhase.DrawInsufficientMaterial => GamePhaseResponse("DrawInsufficientMaterial")
      case GamePhase.Checkmate(winner)        => GamePhaseResponse("Checkmate", Some(colorName(winner)))

  def colorName(color: Color): String =
    color match
      case Color.White => "White"
      case Color.Black => "Black"

  def pieceTypeName(pieceType: PieceType): String =
    pieceType match
      case PieceType.King   => "King"
      case PieceType.Queen  => "Queen"
      case PieceType.Rook   => "Rook"
      case PieceType.Bishop => "Bishop"
      case PieceType.Knight => "Knight"
      case PieceType.Pawn   => "Pawn"

  def moveToUci(move: Move): String =
    val base = s"${move.from.file}${move.from.rank}${move.to.file}${move.to.rank}"
    move.promotion match
      case None              => base
      case Some(PieceType.Queen)  => base + "q"
      case Some(PieceType.Rook)   => base + "r"
      case Some(PieceType.Bishop) => base + "b"
      case Some(PieceType.Knight) => base + "n"
      case Some(_)               => base

  def isCheck(state: ChessState): Boolean =
    state.phase match
      case GamePhase.InProgress => ChessRules.isInCheck(state.board, state.sideToMove, state.enPassant)
      case _                    => false
