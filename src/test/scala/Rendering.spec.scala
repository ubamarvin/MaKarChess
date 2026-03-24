import munit.FunSuite

class RenderingSpec extends FunSuite:
  test("renderPiece uses uppercase for white and lowercase for black") {
    assertEquals(renderPiece(Piece(Color.White, PieceType.Pawn)), 'P')
    assertEquals(renderPiece(Piece(Color.Black, PieceType.Pawn)), 'p')
    assertEquals(renderPiece(Piece(Color.White, PieceType.Knight)), 'N')
    assertEquals(renderPiece(Piece(Color.Black, PieceType.Knight)), 'n')
  }

  test("renderCurrentPlayer returns expected text") {
    assertEquals(renderCurrentPlayer(Color.White), "White to move")
    assertEquals(renderCurrentPlayer(Color.Black), "Black to move")
  }

  test("renderBoard returns strict deterministic layout for initial board") {
    val rendered = renderBoard(initialBoard)
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
