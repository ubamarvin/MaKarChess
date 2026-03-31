package makarchess.util.bot

import makarchess.model.{ChessRules, ChessState, Color, Move, Piece, PieceType, Position}

object BotHeuristics:

  val pieceValue: Map[PieceType, Int] =
    Map(
      PieceType.Pawn -> 1,
      PieceType.Knight -> 3,
      PieceType.Bishop -> 3,
      PieceType.Rook -> 5,
      PieceType.Queen -> 9,
      PieceType.King -> 0
    )

  def opposite(c: Color): Color =
    c match
      case Color.White => Color.Black
      case Color.Black => Color.White

  def capturedPiece(state: ChessState, move: Move): Option[Piece] =
    state.board.get(move.to) match
      case Some(p) if p.color != state.sideToMove => Some(p)
      case _ =>
        val mover = state.board.get(move.from)
        val isEnPassant =
          mover.exists(_.kind == PieceType.Pawn) && state.enPassant.contains(move.to) && state.board.get(move.to).isEmpty

        if !isEnPassant then None
        else
          val capSq =
            Position(
              move.to.file,
              if state.sideToMove == Color.White then move.to.rank - 1 else move.to.rank + 1
            )
          state.board.get(capSq)

  def captureValue(state: ChessState, move: Move): Int =
    capturedPiece(state, move).map(p => pieceValue.getOrElse(p.kind, 0)).getOrElse(0)

  def nextState(state: ChessState, move: Move): ChessState =
    ChessRules.applyLegalMove(state, move)

  def givesCheck(state: ChessState, move: Move): Boolean =
    val next = nextState(state, move)
    ChessRules.isInCheck(next.board, next.sideToMove, next.enPassant)

  def isSquareAttacked(state: ChessState, square: Position, by: Color): Boolean =
    ChessRules.isSquareAttacked(state.board, square, by, state.enPassant)

  def findKing(board: Map[Position, Piece], color: Color): Option[Position] =
    board.collectFirst { case (pos, p) if p.color == color && p.kind == PieceType.King => pos }

  def manhattanDistance(a: Position, b: Position): Int =
    (a.fileIndex - b.fileIndex).abs + (a.rank - b.rank).abs
