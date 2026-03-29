import munit.FunSuite
import makarchess.model.{ChessRules, Color, Piece, PieceType}

class RenderingSpec extends FunSuite:

  test("renderPiece uses uppercase for white and lowercase for black") {
    assertEquals(ChessRules.renderPiece(Piece(Color.White, PieceType.Pawn)), 'P')
    assertEquals(ChessRules.renderPiece(Piece(Color.Black, PieceType.Pawn)), 'p')
    assertEquals(ChessRules.renderPiece(Piece(Color.White, PieceType.Knight)), 'N')
    assertEquals(ChessRules.renderPiece(Piece(Color.Black, PieceType.Knight)), 'n')
  }

  test("renderBoard returns strict deterministic layout for initial board") {
    val rendered = ChessRules.renderBoard(ChessRules.initialState.board)
    val expected =
      """  a b c d e f g h
        |8 r n b q k b n r 8
        |7 p p p p p p p p 7
        |6 . . . . . . . . 6
        |5 . . . . . . . . 5
        |4 . . . . . . . . 4
        |3 . . . . . . . . 3
        |2 P P P P P P P P 2
        |1 R N B Q K B N R 1
        |  a b c d e f g h""".stripMargin

    assertEquals(rendered, expected)
  }
