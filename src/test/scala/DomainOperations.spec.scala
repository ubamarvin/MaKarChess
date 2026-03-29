import munit.FunSuite
import makarchess.model.{ChessRules, Color, Move, Piece, PieceType, Position}

class DomainOperationsSpec extends FunSuite:

  test("initial state has 32 pieces on the board") {
    assertEquals(ChessRules.initialState.board.size, 32)
  }

  test("initial state places kings on e1 and e8") {
    val b = ChessRules.initialState.board
    assertEquals(b.get(Position('e', 1)), Some(Piece(Color.White, PieceType.King)))
    assertEquals(b.get(Position('e', 8)), Some(Piece(Color.Black, PieceType.King)))
  }

  test("legalMoves includes e2e4 from the initial position") {
    val s = ChessRules.initialState
    val m = Move(Position('e', 2), Position('e', 4))
    assert(ChessRules.legalMoves(s).exists(isSameMove(_, m)))
  }

  test("applyLegalMove updates board and switches side after e2e4") {
    val s0 = ChessRules.initialState
    val m = Move(Position('e', 2), Position('e', 4))
    val s1 = ChessRules.applyLegalMove(s0, m)
    assertEquals(s1.sideToMove, Color.Black)
    assertEquals(s1.board.get(Position('e', 2)), None)
    assertEquals(s1.board.get(Position('e', 4)), Some(Piece(Color.White, PieceType.Pawn)))
  }

  private def isSameMove(a: Move, b: Move): Boolean =
    a.from == b.from && a.to == b.to && a.promotion == b.promotion
