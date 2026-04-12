package makarchess.model

case class Fen(
    board: Map[Position, Piece],
    sideToMove: Color,
    castling: CastlingRights,
    enPassant: Option[Position],
    halfmoveClock: Int,
    fullmoveNumber: Int
)

object Fen:
  def toChessState(fen: Fen): Either[String, ChessState] =
    val whiteKings = fen.board.values.count(p => p.color == Color.White && p.kind == PieceType.King)
    val blackKings = fen.board.values.count(p => p.color == Color.Black && p.kind == PieceType.King)

    if whiteKings != 1 || blackKings != 1 then Left("FEN must contain exactly one white king and one black king.")
    else
      val base = ChessState(
        board = fen.board,
        sideToMove = fen.sideToMove,
        castling = fen.castling,
        enPassant = fen.enPassant,
        halfmoveClock = fen.halfmoveClock,
        fullmoveNumber = fen.fullmoveNumber,
        repetitionHistory = Nil,
        phase = GamePhase.InProgress
      )
      Right(ChessRules.evaluateState(base))
