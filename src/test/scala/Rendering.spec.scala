import munit.FunSuite
import makarchess.model.{ChessRules, Color, Piece, PieceType}

class RenderingSpec extends FunSuite:

  test("renderPiece uses unicode chess symbols") {
    assertEquals(ChessRules.renderPiece(Piece(Color.White, PieceType.Pawn)), '♙')
    assertEquals(ChessRules.renderPiece(Piece(Color.Black, PieceType.Pawn)), '♟')
    assertEquals(ChessRules.renderPiece(Piece(Color.White, PieceType.Knight)), '♘')
    assertEquals(ChessRules.renderPiece(Piece(Color.Black, PieceType.Knight)), '♞')
  }

  test("renderBoard returns strict deterministic layout for initial board") {
    val rendered = ChessRules.renderBoard(ChessRules.initialState.board)
    val expected =
      """  a b c d e f g h
        |8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜ 8
        |7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟ 7
        |6 . . . . . . . . 6
        |5 . . . . . . . . 5
        |4 . . . . . . . . 4
        |3 . . . . . . . . 3
        |2 ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙ 2
        |1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖ 1
        |  a b c d e f g h""".stripMargin

    assertEquals(rendered, expected)
  }
