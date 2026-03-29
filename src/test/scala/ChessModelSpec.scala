import munit.FunSuite

import makarchess.model.{CastlingRights, ChessModel, ChessRules, ChessState, Color, GamePhase, MoveAttemptError, Piece, PieceType, Position, PositionKey}
import makarchess.util.Observer

class ChessModelSpec extends FunSuite:

  private def baseState(
      board: Map[Position, Piece],
      sideToMove: Color,
      phase: GamePhase
  ): ChessState =
    val castling = CastlingRights(false, false, false, false)
    val enPassant = None
    val key = PositionKey(board, sideToMove, castling, enPassant)
    ChessState(
      board = board,
      sideToMove = sideToMove,
      castling = castling,
      enPassant = enPassant,
      halfmoveClock = 0,
      fullmoveNumber = 1,
      repetitionHistory = List(key),
      phase = phase
    )

  test("restart notifies observers") {
    val model0 = ChessModel()
    var updates = 0
    val observer = new Observer:
      override def update: Unit = updates += 1

    model0.add(observer)
    val model1 = model0.restart()
    model1.notifyObservers

    assertEquals(updates, 1)
  }

  test("snapshot InProgress reports Check when king is attacked") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.Rook)
    )
    val model = ChessModel.fromState(baseState(board, Color.White, GamePhase.InProgress))

    val snap = model.snapshot
    assertEquals(snap.statusLine, "Check.")
    assertEquals(snap.currentPlayerLine, "White to move")
  }

  test("snapshot reports Checkmate/Stalemate/Draw variants") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('a', 8) -> Piece(Color.Black, PieceType.King)
    )

    val mMate = ChessModel.fromState(baseState(board, Color.White, GamePhase.Checkmate(Color.White)))
    assertEquals(mMate.snapshot.statusLine, "Checkmate. White wins.")
    assertEquals(mMate.snapshot.currentPlayerLine, "")

    val mStale = ChessModel.fromState(baseState(board, Color.White, GamePhase.Stalemate))
    assertEquals(mStale.snapshot.statusLine, "Stalemate. Draw.")
    assertEquals(mStale.snapshot.currentPlayerLine, "")

    val m50 = ChessModel.fromState(baseState(board, Color.White, GamePhase.DrawFiftyMoveRule))
    assertEquals(m50.snapshot.statusLine, "Draw by fifty-move rule.")
    assertEquals(m50.snapshot.currentPlayerLine, "")

    val m3 = ChessModel.fromState(baseState(board, Color.White, GamePhase.DrawThreefoldRepetition))
    assertEquals(m3.snapshot.statusLine, "Draw by threefold repetition.")
    assertEquals(m3.snapshot.currentPlayerLine, "")

    val mIns = ChessModel.fromState(baseState(board, Color.White, GamePhase.DrawInsufficientMaterial))
    assertEquals(mIns.snapshot.statusLine, "Draw by insufficient material.")
    assertEquals(mIns.snapshot.currentPlayerLine, "")
  }

  test("tryMove returns GameAlreadyOver when phase is not InProgress") {
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('a', 8) -> Piece(Color.Black, PieceType.King)
    )
    val model = ChessModel.fromState(baseState(board, Color.White, GamePhase.Stalemate))

    val res = model.tryMove("e2e4")
    assertEquals(res, Left(MoveAttemptError.GameAlreadyOver))
  }

end ChessModelSpec

