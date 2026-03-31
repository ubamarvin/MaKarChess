import munit.FunSuite

import makarchess.model.{CastlingRights, ChessModel, ChessRules, ChessState, Color, GamePhase, Move, Piece, PieceType, Position, PositionKey}
import makarchess.util.bot.{AggressiveBot, BotFactory, DefensiveBot, GreedyBot}

class MoreBotsSpec extends FunSuite:

  private def stateFor(
      board: Map[Position, Piece],
      sideToMove: Color,
      castling: CastlingRights = CastlingRights(false, false, false, false),
      enPassant: Option[Position] = None,
      phase: GamePhase = GamePhase.InProgress
  ): ChessState =
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

  test("BotFactory maps names to bot instances") {
    assert(BotFactory.fromName("random").isRight)
    assert(BotFactory.fromName("greedy").isRight)
    assert(BotFactory.fromName("defensive").isRight)
    assert(BotFactory.fromName("aggressive").isRight)

    assertEquals(BotFactory.fromName("unknown"), Left("Unknown bot type: unknown"))
  }

  test("GreedyBot prefers higher-value capture") {
    // White queen can capture either a pawn or a rook; should choose rook.
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('d', 1) -> Piece(Color.White, PieceType.Queen),
      Position('d', 7) -> Piece(Color.Black, PieceType.Pawn),
      Position('a', 1) -> Piece(Color.Black, PieceType.Rook)
    )

    val state = stateFor(board, Color.White)
    val bot = GreedyBot()

    val mv = bot.chooseMove(state).get
    val captured = state.board.get(mv.to)
    assertEquals(captured.map(_.kind), Some(PieceType.Rook))
  }

  test("AggressiveBot prefers giving check when available") {
    // White rook can move to e7 giving check to black king on e8.
    val board = Map(
      Position('e', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('e', 2) -> Piece(Color.White, PieceType.Rook)
    )

    val state = stateFor(board, Color.White)
    val bot = AggressiveBot()

    val mv = bot.chooseMove(state).get
    val next = ChessRules.applyLegalMove(state, mv)
    assert(ChessRules.isInCheck(next.board, next.sideToMove, next.enPassant))
  }

  test("DefensiveBot prefers moving a threatened piece to safety") {
    // Simple case: white knight is attacked by a black rook. The knight is NOT pinned to the king,
    // so moving it away should be a legal way to save material.
    val board = Map(
      Position('a', 1) -> Piece(Color.White, PieceType.King),
      Position('e', 8) -> Piece(Color.Black, PieceType.King),
      Position('e', 4) -> Piece(Color.White, PieceType.Knight),
      Position('e', 7) -> Piece(Color.Black, PieceType.Rook)
    )

    val state = stateFor(board, Color.White)
    val bot = DefensiveBot()

    val mv = bot.chooseMove(state).get
    // must move the knight (the threatened piece)
    assertEquals(mv.from, Position('e', 4))

    val next = ChessRules.applyLegalMove(state, mv)
    val enemy = Color.Black
    assert(!ChessRules.isSquareAttacked(next.board, mv.to, enemy, next.enPassant))
  }
