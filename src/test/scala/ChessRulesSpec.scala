import munit.FunSuite

import makarchess.model.{CastlingRights, ChessRules, ChessState, Color, GamePhase, Move, MoveAttemptError, Piece, PieceType, Position, PositionKey}
import makarchess.util.MoveResult

class ChessRulesSpec extends FunSuite:

  private def stateFor(
      board: Map[Position, Piece],
      sideToMove: Color,
      castling: CastlingRights,
      enPassant: Option[Position],
      halfmoveClock: Int = 0,
      repetitionHistory: List[PositionKey] = Nil,
      phase: GamePhase = GamePhase.InProgress
  ): ChessState =
    val key = PositionKey(board, sideToMove, castling, enPassant)
    ChessState(
      board = board,
      sideToMove = sideToMove,
      castling = castling,
      enPassant = enPassant,
      halfmoveClock = halfmoveClock,
      fullmoveNumber = 1,
      repetitionHistory = if repetitionHistory.nonEmpty then repetitionHistory else List(key),
      phase = phase
    )

  private val noCastle: CastlingRights = CastlingRights(false, false, false, false)

  test("parseUci rejects invalid promotion character") {
    val result = ChessRules.parseUci("e7e8x")
    assertEquals(result, MoveResult.Err(MoveAttemptError.InvalidInput))
  }

  test("promotion move updates piece kind on applyLegalMove") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('a', 8) -> Piece(Color.Black, PieceType.King),
      Position('e', 7) -> Piece(Color.White, PieceType.Pawn)
    )
    val s = stateFor(board, Color.White, noCastle, None)

    assert(ChessRules.legalMoves(s).exists(_.promotion.contains(PieceType.Queen)))

    val next = ChessRules.applyLegalMove(s, Move(Position('e', 7), Position('e', 8), Some(PieceType.Queen)))
    assertEquals(next.board(Position('e', 8)).kind, PieceType.Queen)
    assertEquals(next.board.get(Position('e', 7)), None)
  }

  test("en passant capture removes captured pawn") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('e', 5) -> Piece(Color.White, PieceType.Pawn),
      Position('d', 5) -> Piece(Color.Black, PieceType.Pawn)
    )
    val epTarget = Position('d', 6) // target square pawn moves to
    val s = stateFor(board, Color.White, noCastle, Some(epTarget))

    val epMove = Move(Position('e', 5), Position('d', 6), None)
    assert(ChessRules.legalMoves(s).contains(epMove))

    val next = ChessRules.applyLegalMove(s, epMove)
    assertEquals(next.board.get(Position('d', 5)), None) // captured pawn removed
    assertEquals(next.board.get(Position('d', 6)), Some(Piece(Color.White, PieceType.Pawn)))
    assertEquals(next.enPassant, None) // EP expires after the move
    assertEquals(next.sideToMove, Color.Black)
  }

  test("white kingside and queenside castling relocates rooks") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('h', 1) -> Piece(Color.White, PieceType.Rook),
      Position('a', 1) -> Piece(Color.White, PieceType.Rook)
    )
    val s = stateFor(
      board,
      sideToMove = Color.White,
      castling = CastlingRights(true, true, false, false),
      enPassant = None
    )

    assert(ChessRules.legalMoves(s).contains(Move(Position('e', 1), Position('g', 1), None)))
    assert(ChessRules.legalMoves(s).contains(Move(Position('e', 1), Position('c', 1), None)))

    val nextK = ChessRules.applyLegalMove(s, Move(Position('e', 1), Position('g', 1), None))
    assertEquals(nextK.board.get(Position('h', 1)), None)
    assertEquals(nextK.board.get(Position('f', 1)), Some(Piece(Color.White, PieceType.Rook)))

    val nextQ = ChessRules.applyLegalMove(s, Move(Position('e', 1), Position('c', 1), None))
    assertEquals(nextQ.board.get(Position('a', 1)), None)
    assertEquals(nextQ.board.get(Position('d', 1)), Some(Piece(Color.White, PieceType.Rook)))
  }

  test("castling is disallowed when king is in check") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.Rook),
      Position('h', 1) -> Piece(Color.White, PieceType.Rook)
    )
    val s = stateFor(
      board,
      sideToMove = Color.White,
      castling = CastlingRights(true, false, false, false),
      enPassant = None
    )

    assert(!ChessRules.legalMoves(s).contains(Move(Position('e', 1), Position('g', 1), None)))
  }

  test("black queenside castling relocates rook") {
    val board = Map(
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('a', 8) -> Piece(Color.Black, PieceType.Rook)
    )
    val s = stateFor(
      board,
      sideToMove = Color.Black,
      castling = CastlingRights(false, false, false, true),
      enPassant = None
    )

    assert(ChessRules.legalMoves(s).contains(Move(Position('e', 8), Position('c', 8), None)))
    val next = ChessRules.applyLegalMove(s, Move(Position('e', 8), Position('c', 8), None))
    assertEquals(next.board.get(Position('a', 8)), None)
    assertEquals(next.board.get(Position('d', 8)), Some(Piece(Color.Black, PieceType.Rook)))
  }

  test("stalemate is detected when side to move has no legal moves and is not in check") {
    // After the move b5->b6, black to move (king a8) should be stalemated.
    val board = Map(
      Position('c', 6) -> Piece(Color.White, PieceType.King),
      Position('b', 5) -> Piece(Color.White, PieceType.Queen),
      Position('a', 8) -> Piece(Color.Black, PieceType.King)
    )
    val s = stateFor(board, Color.White, noCastle, None)

    val next = ChessRules.applyLegalMove(s, Move(Position('b', 5), Position('b', 6), None))
    assertEquals(next.phase, GamePhase.Stalemate)
  }

  test("checkmate is detected when side to move has no legal moves and is in check") {
    // After the move b6->b7, black king at a8 should be checkmated.
    val board = Map(
      Position('c', 6) -> Piece(Color.White, PieceType.King),
      Position('b', 6) -> Piece(Color.White, PieceType.Queen),
      Position('a', 8) -> Piece(Color.Black, PieceType.King)
    )
    val s = stateFor(board, Color.White, noCastle, None)

    val next = ChessRules.applyLegalMove(s, Move(Position('b', 6), Position('b', 7), None))
    assertEquals(next.phase, GamePhase.Checkmate(Color.White))
  }

  test("fifty-move rule triggers when halfmoveClock reaches 100+") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('g', 1) -> Piece(Color.White, PieceType.Knight)
    )
    val s = stateFor(board, Color.White, noCastle, None, halfmoveClock = 100)

    val next = ChessRules.applyLegalMove(s, Move(Position('g', 1), Position('f', 3), None))
    assertEquals(next.phase, GamePhase.DrawFiftyMoveRule)
  }

  test("threefold repetition triggers when repetitionHistory contains the new key twice already") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('h', 1) -> Piece(Color.White, PieceType.Rook),
      Position('g', 1) -> Piece(Color.White, PieceType.Knight)
    )
    val move = Move(Position('g', 1), Position('f', 3), None)
    val nextBoard = board - move.from + (move.to -> board(move.from))
    val nextSide = Color.Black
    val nextKey = PositionKey(nextBoard, nextSide, noCastle, None)

    val s = stateFor(
      board,
      sideToMove = Color.White,
      castling = noCastle,
      enPassant = None,
      halfmoveClock = 0,
      repetitionHistory = List(nextKey, nextKey)
    )

    val next = ChessRules.applyLegalMove(s, move)
    assertEquals(next.phase, GamePhase.DrawThreefoldRepetition)
  }

  test("insufficient material draws cover cases: <=2, ==3, and ==4 patterns") {
    // <= 2 pieces: only kings.
    val s2 = stateFor(
      board = Map(
        Position('e', 1) -> Piece(Color.White, PieceType.King),
        Position('e', 8) -> Piece(Color.Black, PieceType.King)
      ),
      sideToMove = Color.White,
      castling = noCastle,
      enPassant = None
    )
    val next2 = ChessRules.applyLegalMove(s2, Move(Position('e', 1), Position('d', 1), None))
    assertEquals(next2.phase, GamePhase.DrawInsufficientMaterial)

    // == 3 pieces: kings + bishop.
    val s3 = stateFor(
      board = Map(
        Position('e', 1) -> Piece(Color.White, PieceType.King),
        Position('e', 8) -> Piece(Color.Black, PieceType.King),
        Position('b', 4) -> Piece(Color.White, PieceType.Bishop)
      ),
      sideToMove = Color.White,
      castling = noCastle,
      enPassant = None
    )
    val next3 = ChessRules.applyLegalMove(s3, Move(Position('e', 1), Position('d', 1), None))
    assertEquals(next3.phase, GamePhase.DrawInsufficientMaterial)

    // == 4 pieces pattern: two knights.
    val s4n = stateFor(
      board = Map(
        Position('e', 1) -> Piece(Color.White, PieceType.King),
        Position('e', 8) -> Piece(Color.Black, PieceType.King),
        Position('b', 1) -> Piece(Color.White, PieceType.Knight),
        Position('g', 1) -> Piece(Color.Black, PieceType.Knight)
      ),
      sideToMove = Color.White,
      castling = noCastle,
      enPassant = None
    )
    val next4n = ChessRules.applyLegalMove(s4n, Move(Position('b', 1), Position('c', 3), None))
    assertEquals(next4n.phase, GamePhase.DrawInsufficientMaterial)

    // == 4 pieces pattern: two bishops.
    val s4b = stateFor(
      board = Map(
        Position('e', 1) -> Piece(Color.White, PieceType.King),
        Position('e', 8) -> Piece(Color.Black, PieceType.King),
        Position('b', 1) -> Piece(Color.White, PieceType.Bishop),
        Position('g', 1) -> Piece(Color.Black, PieceType.Bishop)
      ),
      sideToMove = Color.White,
      castling = noCastle,
      enPassant = None
    )
    val next4b = ChessRules.applyLegalMove(s4b, Move(Position('b', 1), Position('c', 2), None))
    assertEquals(next4b.phase, GamePhase.DrawInsufficientMaterial)
  }

end ChessRulesSpec

