import munit.FunSuite

class DomainOperationsSpec extends FunSuite:
  test("initialBoard has 32 pieces") {
    assertEquals(initialBoard.pieces.size, 32)
  }

  test("initialBoard places kings correctly") {
    assertEquals(pieceAt(initialBoard, Position('e', 1)), Some(Piece(Color.White, PieceType.King)))
    assertEquals(pieceAt(initialBoard, Position('e', 8)), Some(Piece(Color.Black, PieceType.King)))
  }

  test("pieceAt returns None for empty square") {
    assertEquals(pieceAt(initialBoard, Position('e', 4)), None)
  }

  test("placePiece adds a piece to board") {
    val board = Board(Map.empty)
    val updated = placePiece(board, Position('d', 4), Piece(Color.White, PieceType.Queen))

    assertEquals(pieceAt(updated, Position('d', 4)), Some(Piece(Color.White, PieceType.Queen)))
  }

  test("removePiece removes piece from board") {
    val board = Board(Map(Position('d', 4) -> Piece(Color.White, PieceType.Queen)))
    val updated = removePiece(board, Position('d', 4))

    assertEquals(pieceAt(updated, Position('d', 4)), None)
  }

  test("relocatePiece moves piece and clears source") {
    val board = Board(Map(Position('a', 2) -> Piece(Color.White, PieceType.Pawn)))
    val updated = relocatePiece(board, Move(Position('a', 2), Position('a', 3)))

    assertEquals(pieceAt(updated, Position('a', 2)), None)
    assertEquals(pieceAt(updated, Position('a', 3)), Some(Piece(Color.White, PieceType.Pawn)))
  }

  test("opposite flips colors") {
    assertEquals(opposite(Color.White), Color.Black)
    assertEquals(opposite(Color.Black), Color.White)
  }

  test("initialGameState starts with white to move") {
    assertEquals(initialGameState.currentPlayer, Color.White)
  }

  test("applyMove fails when source square is empty") {
    val result = applyMove(initialGameState, Move(Position('e', 4), Position('e', 5)))

    assertEquals(result, Left(GameError.NoPieceAtSource))
  }

  test("applyMove fails when moving wrong player's piece") {
    val state = initialGameState.copy(currentPlayer = Color.Black)
    val result = applyMove(state, Move(Position('e', 2), Position('e', 4)))

    assertEquals(result, Left(GameError.WrongPlayer))
  }

  test("applyMove rejects same-color capture") {
    val custom = Board(
      Map(
        Position('a', 2) -> Piece(Color.White, PieceType.Pawn),
        Position('a', 3) -> Piece(Color.White, PieceType.Knight)
      )
    )
    val state = GameState(custom, Color.White)

    val result = applyMove(state, Move(Position('a', 2), Position('a', 3)))
    assertEquals(result, Left(GameError.SameColorCapture))
  }

  test("applyMove succeeds and switches turn") {
    val result = applyMove(initialGameState, Move(Position('e', 2), Position('e', 4)))

    assert(result.isRight)
    val next = result.toOption.get
    assertEquals(next.currentPlayer, Color.Black)
    assertEquals(pieceAt(next.board, Position('e', 2)), None)
    assertEquals(pieceAt(next.board, Position('e', 4)), Some(Piece(Color.White, PieceType.Pawn)))
  }

  test("failed applyMove does not change turn") {
    val result = applyMove(initialGameState, Move(Position('e', 4), Position('e', 5)))
    assertEquals(result, Left(GameError.NoPieceAtSource))
    assertEquals(initialGameState.currentPlayer, Color.White)
  }
