package makarchess.model

import scala.annotation.tailrec
import makarchess.util.MoveResult

/** Pure chess rules: move generation, legality, and state transitions. */
object ChessRules:

  def initialState: ChessState =
    val board = initialBoardMap
    val key = PositionKey(board, Color.White, CastlingRights.initial, None)
    ChessState(
      board = board,
      sideToMove = Color.White,
      castling = CastlingRights.initial,
      enPassant = None,
      halfmoveClock = 0,
      fullmoveNumber = 1,
      repetitionHistory = List(key),
      phase = GamePhase.InProgress
    )

  private def initialBoardMap: Map[Position, Piece] =
    val whiteBackRank = List(
      'a' -> Piece(Color.White, PieceType.Rook),
      'b' -> Piece(Color.White, PieceType.Knight),
      'c' -> Piece(Color.White, PieceType.Bishop),
      'd' -> Piece(Color.White, PieceType.Queen),
      'e' -> Piece(Color.White, PieceType.King),
      'f' -> Piece(Color.White, PieceType.Bishop),
      'g' -> Piece(Color.White, PieceType.Knight),
      'h' -> Piece(Color.White, PieceType.Rook)
    )
    val blackBackRank = whiteBackRank.map { case (f, p) => f -> p.copy(color = Color.Black) }
    val whitePawns = ('a' to 'h').map(f => Position(f, 2) -> Piece(Color.White, PieceType.Pawn))
    val blackPawns = ('a' to 'h').map(f => Position(f, 7) -> Piece(Color.Black, PieceType.Pawn))
    val whiteBack = whiteBackRank.map { case (f, p) => Position(f, 1) -> p }
    val blackBack = blackBackRank.map { case (f, p) => Position(f, 8) -> p }
    (whiteBack ++ blackBack ++ whitePawns ++ blackPawns).toMap

  def parseUci(input: String): MoveResult[Move] =
    val s = input.trim.toLowerCase
    if s.length != 4 && s.length != 5 then MoveResult.fail(MoveAttemptError.InvalidInput)
    else
      val promo =
        if s.length == 5 then
          s(4) match
            case 'q' => Some(PieceType.Queen)
            case 'r' => Some(PieceType.Rook)
            case 'b' => Some(PieceType.Bishop)
            case 'n' => Some(PieceType.Knight)
            case _   => None
        else None
      if s.length == 5 && promo.isEmpty then MoveResult.fail(MoveAttemptError.InvalidInput)
      else
        def toPos(file: Char, rank: Int): MoveResult[Position] =
          val f = file.toLower
          if f >= 'a' && f <= 'h' && rank >= 1 && rank <= 8 then MoveResult.pure(Position(f, rank))
          else MoveResult.fail(MoveAttemptError.InvalidInput)

        for
          from <- toPos(s(0), s(1).asDigit)
          to <- toPos(s(2), s(3).asDigit)
        yield Move(from, to, promo)

  def renderBoard(board: Map[Position, Piece]): String =
    val files = ('a' to 'h').mkString(" ")
    val rows = (8 to 1 by -1).map { rank =>
      val squares = ('a' to 'h').map { file =>
        val pos = Position(file, rank)
        board.get(pos).map(renderPiece).getOrElse('.')
      }.mkString(" ")
      s"$rank $squares $rank"
    }
    (Seq(s"  $files") ++ rows ++ Seq(s"  $files")).mkString("\n")

  def renderPiece(piece: Piece): Char =
    (piece.color, piece.kind) match
      case (Color.White, PieceType.King)   => '♔'
      case (Color.White, PieceType.Queen)  => '♕'
      case (Color.White, PieceType.Rook)   => '♖'
      case (Color.White, PieceType.Bishop) => '♗'
      case (Color.White, PieceType.Knight) => '♘'
      case (Color.White, PieceType.Pawn)   => '♙'
      case (Color.Black, PieceType.King)   => '♚'
      case (Color.Black, PieceType.Queen)  => '♛'
      case (Color.Black, PieceType.Rook)   => '♜'
      case (Color.Black, PieceType.Bishop) => '♝'
      case (Color.Black, PieceType.Knight) => '♞'
      case (Color.Black, PieceType.Pawn)   => '♟'

  def legalMoves(state: ChessState): List[Move] =
    pseudoLegalMoves(state).filter(m => !leavesOwnKingInCheck(state, m))

  def applyLegalMove(state: ChessState, move: Move): ChessState =
    val raw = applyMoveUnchecked(state, move)
    finalizeAfterMove(raw)

  def evaluateState(raw: ChessState): ChessState =
    val baseHistory =
      if raw.repetitionHistory.isEmpty then List(raw.positionKey)
      else raw.repetitionHistory
    finalizeAfterMove(raw.copy(repetitionHistory = baseHistory.dropRight(1), phase = GamePhase.InProgress))

  private def finalizeAfterMove(raw: ChessState): ChessState =
    val withHistory = raw.copy(repetitionHistory = raw.repetitionHistory :+ raw.positionKey)
    val phase1 =
      if withHistory.halfmoveClock >= 100 then GamePhase.DrawFiftyMoveRule
      else if withHistory.repetitionHistory.count(_ == withHistory.positionKey) >= 3 then
        GamePhase.DrawThreefoldRepetition
      else if insufficientMaterial(withHistory.board) then GamePhase.DrawInsufficientMaterial
      else GamePhase.InProgress

    if phase1 != GamePhase.InProgress then withHistory.copy(phase = phase1)
    else
      val lm = legalMoves(withHistory)
      val inCheck = isInCheck(withHistory.board, withHistory.sideToMove, withHistory.enPassant)
      val endPhase =
        if lm.isEmpty then
          if inCheck then GamePhase.Checkmate(opposite(withHistory.sideToMove))
          else GamePhase.Stalemate
        else GamePhase.InProgress
      withHistory.copy(phase = endPhase)

  private def insufficientMaterial(board: Map[Position, Piece]): Boolean =
    val pieces = board.values.toList
    if pieces.exists(p =>
        p.kind == PieceType.Pawn || p.kind == PieceType.Rook || p.kind == PieceType.Queen
      )
    then false
    else
      pieces.size match
        case n if n <= 2 => true
        case 3           => true
        case 4 =>
          val knights = pieces.count(_.kind == PieceType.Knight)
          val bishops = pieces.count(_.kind == PieceType.Bishop)
          (knights == 2 && bishops == 0) || (knights == 0 && bishops == 2)
        case _ => false

  private def opposite(c: Color): Color =
    c match
      case Color.White => Color.Black
      case Color.Black => Color.White

  def isInCheck(board: Map[Position, Piece], forSide: Color, enPassant: Option[Position]): Boolean =
    findKing(board, forSide) match
      case None => false
      case Some(k) =>
        isSquareAttacked(board, k, opposite(forSide), enPassant)

  def isSquareAttacked(
      board: Map[Position, Piece],
      square: Position,
      by: Color,
      enPassant: Option[Position]
  ): Boolean =
    board.exists { case (pos, piece) =>
      piece.color == by && attacksSquare(board, pos, square, piece, enPassant)
    }

  private def leavesOwnKingInCheck(state: ChessState, move: Move): Boolean =
    val next = applyMoveUnchecked(state, move)
    isInCheck(next.board, state.sideToMove, next.enPassant)

  private def findKing(board: Map[Position, Piece], color: Color): Option[Position] =
    board.collectFirst { case (pos, p) if p.kind == PieceType.King && p.color == color => pos }

  private def attacksSquare(
      board: Map[Position, Piece],
      from: Position,
      target: Position,
      piece: Piece,
      enPassant: Option[Position]
  ): Boolean =
    piece.kind match
      case PieceType.Pawn   => pawnAttacks(from, piece.color, target, board, enPassant)
      case PieceType.Knight => knightAttacks(from, target)
      case PieceType.Bishop => slidingAttack(board, from, target, piece.color, diagonal = true)
      case PieceType.Rook   => slidingAttack(board, from, target, piece.color, diagonal = false)
      case PieceType.Queen =>
        slidingAttack(board, from, target, piece.color, diagonal = true) ||
          slidingAttack(board, from, target, piece.color, diagonal = false)
      case PieceType.King => kingAttacks(from, target)

  private def pawnAttacks(
      from: Position,
      color: Color,
      target: Position,
      board: Map[Position, Piece],
      enPassant: Option[Position]
  ): Boolean =
    val df = target.file - from.file
    val dr = target.rank - from.rank
    color match
      case Color.White =>
        if dr != 1 || math.abs(df) != 1 then false
        else if board.contains(target) then board(target).color == Color.Black
        else
          enPassant.contains(target) && board
            .get(Position(target.file, target.rank - 1))
            .exists(p => p.kind == PieceType.Pawn && p.color == Color.Black)
      case Color.Black =>
        if dr != -1 || math.abs(df) != 1 then false
        else if board.contains(target) then board(target).color == Color.White
        else
          enPassant.contains(target) && board
            .get(Position(target.file, target.rank + 1))
            .exists(p => p.kind == PieceType.Pawn && p.color == Color.White)

  private def knightAttacks(from: Position, to: Position): Boolean =
    val df = math.abs(from.file - to.file)
    val dr = math.abs(from.rank - to.rank)
    (df == 1 && dr == 2) || (df == 2 && dr == 1)

  private def kingAttacks(from: Position, to: Position): Boolean =
    val df = math.abs(from.file - to.file)
    val dr = math.abs(from.rank - to.rank)
    df <= 1 && dr <= 1 && (df + dr) > 0

  private def slidingAttack(
      board: Map[Position, Piece],
      from: Position,
      target: Position,
      color: Color,
      diagonal: Boolean
  ): Boolean =
    val df = Integer.signum(target.file - from.file)
    val dr = Integer.signum(target.rank - from.rank)
    if df == 0 && dr == 0 then false
    else if diagonal && (df == 0 || dr == 0) then false
    else if !diagonal && df != 0 && dr != 0 then false
    else
      step(from, df, dr) match
        case None      => false
        case Some(first) => rayClear(board, first, target, df, dr, color)

  @tailrec
  private def rayClear(
      board: Map[Position, Piece],
      cur: Position,
      target: Position,
      df: Int,
      dr: Int,
      color: Color
  ): Boolean =
    if cur == target then
      board.get(target) match
        case None    => true
        case Some(p) => p.color != color
    else if board.contains(cur) then false
    else
      step(cur, df, dr) match
        case Some(nxt) => rayClear(board, nxt, target, df, dr, color)
        case None      => false

  private def step(from: Position, df: Int, dr: Int): Option[Position] =
    val nf = from.file + df
    val nr = from.rank + dr
    if nf >= 'a' && nf <= 'h' && nr >= 1 && nr <= 8 then Some(Position(nf.toChar, nr))
    else None

  private def pseudoLegalMoves(state: ChessState): List[Move] =
    val b = state.board
    val side = state.sideToMove
    b.toList
      .filter(_._2.color == side)
      .flatMap { case (pos, piece) =>
        piece.kind match
          case PieceType.Pawn   => pawnMoves(state, pos, piece)
          case PieceType.Knight => knightMoves(state, pos, piece)
          case PieceType.Bishop => slidingMoves(state, pos, piece, diagonals = true)
          case PieceType.Rook   => slidingMoves(state, pos, piece, diagonals = false)
          case PieceType.Queen =>
            slidingMoves(state, pos, piece, diagonals = true) ++
              slidingMoves(state, pos, piece, diagonals = false)
          case PieceType.King => kingMoves(state, pos, piece)
      }

  private def pawnMoves(state: ChessState, from: Position, piece: Piece): List[Move] =
    val b = state.board
    val ep = state.enPassant
    val dir = if piece.color == Color.White then 1 else -1
    val startRank = if piece.color == Color.White then 2 else 7
    val promoteRank = if piece.color == Color.White then 8 else 1

    def addPromotions(to: Position): List[Move] =
      if to.rank == promoteRank then
        List(PieceType.Queen, PieceType.Rook, PieceType.Bishop, PieceType.Knight).map(pr =>
          Move(from, to, Some(pr))
        )
      else List(Move(from, to, None))

    val one = Position(from.file, from.rank + dir)
    val forward =
      if one.isValid && !b.contains(one) then
        if from.rank + dir == promoteRank then addPromotions(one)
        else List(Move(from, one, None))
      else Nil

    val two =
      if from.rank == startRank then
        val mid = Position(from.file, from.rank + dir)
        val dest = Position(from.file, from.rank + 2 * dir)
        if mid.isValid && dest.isValid && !b.contains(mid) && !b.contains(dest) then
          List(Move(from, dest, None))
        else Nil
      else Nil

    val captures =
      List(-1, 1).flatMap { df =>
        val to = Position((from.file + df).toChar, from.rank + dir)
        if !to.isValid then Nil
        else
          b.get(to) match
            case Some(target) if target.color != piece.color =>
              if to.rank == promoteRank then addPromotions(to) else List(Move(from, to, None))
            case None if ep.contains(to) =>
              val capRank = if piece.color == Color.White then to.rank - 1 else to.rank + 1
              val cap = Position(to.file, capRank)
              if b.get(cap).exists(p => p.kind == PieceType.Pawn && p.color != piece.color) then
                List(Move(from, to, None))
              else Nil
            case _ => Nil
      }

    forward ++ two ++ captures

  private def knightMoves(state: ChessState, from: Position, piece: Piece): List[Move] =
    val offsets = List(
      (1, 2),
      (1, -2),
      (-1, 2),
      (-1, -2),
      (2, 1),
      (2, -1),
      (-2, 1),
      (-2, -1)
    )
    offsets.flatMap { case (df, dr) =>
      val to = Position((from.file + df).toChar, from.rank + dr)
      if to.isValid && !state.board.get(to).exists(_.color == piece.color) then
        Some(Move(from, to, None))
      else None
    }

  private def slidingMoves(
      state: ChessState,
      from: Position,
      piece: Piece,
      diagonals: Boolean
  ): List[Move] =
    val dirs =
      if diagonals then List((1, 1), (1, -1), (-1, 1), (-1, -1))
      else List((1, 0), (-1, 0), (0, 1), (0, -1))
    dirs.flatMap { case (df, dr) => collectRay(state, from, piece, df, dr) }

  @tailrec
  private def collectRay(
      state: ChessState,
      origin: Position,
      cursor: Position,
      piece: Piece,
      df: Int,
      dr: Int,
      acc: List[Move]
  ): List[Move] =
    step(cursor, df, dr) match
      case None => acc.reverse
      case Some(to) =>
        state.board.get(to) match
          case None => collectRay(state, origin, to, piece, df, dr, Move(origin, to, None) :: acc)
          case Some(target) =>
            if target.color != piece.color then (Move(origin, to, None) :: acc).reverse
            else acc.reverse

  private def collectRay(
      state: ChessState,
      from: Position,
      piece: Piece,
      df: Int,
      dr: Int
  ): List[Move] =
    collectRay(state, from, from, piece, df, dr, Nil)

  private def kingMoves(state: ChessState, from: Position, piece: Piece): List[Move] =
    val neighbors =
      for
        df <- -1 to 1
        dr <- -1 to 1
        if df != 0 || dr != 0
        to = Position((from.file + df).toChar, from.rank + dr)
        if to.isValid && !state.board.get(to).exists(_.color == piece.color)
      yield Move(from, to, None)

    neighbors.toList ++ castlingMoves(state, from, piece)

  private def castlingMoves(state: ChessState, from: Position, piece: Piece): List[Move] =
    if piece.kind != PieceType.King || piece.color != state.sideToMove then Nil
    else if isInCheck(state.board, piece.color, state.enPassant) then Nil
    else
      val rank = if piece.color == Color.White then 1 else 8
      if from != Position('e', rank) then Nil
      else
        val c = state.castling
        val enemy = opposite(piece.color)

        // Functional style: build optional moves and keep only the ones whose conditions are satisfied.
        def kingSideAllowed(right: Boolean): Boolean =
          right && squaresEmpty(state.board, List(Position('f', rank), Position('g', rank))) &&
            !isSquareAttacked(state.board, Position('f', rank), enemy, state.enPassant) &&
            !isSquareAttacked(state.board, Position('g', rank), enemy, state.enPassant)

        def queenSideAllowed(right: Boolean): Boolean =
          right && squaresEmpty(state.board, List(Position('b', rank), Position('c', rank), Position('d', rank))) &&
            !isSquareAttacked(state.board, Position('d', rank), enemy, state.enPassant) &&
            !isSquareAttacked(state.board, Position('c', rank), enemy, state.enPassant)

        val (ksRight, qsRight) =
          piece.color match
            case Color.White => (c.whiteKingside, c.whiteQueenside)
            case Color.Black => (c.blackKingside, c.blackQueenside)

        List(
          Option.when(kingSideAllowed(ksRight))(Move(from, Position('g', rank), None)),
          Option.when(queenSideAllowed(qsRight))(Move(from, Position('c', rank), None))
        ).flatten

  private def squaresEmpty(board: Map[Position, Piece], squares: List[Position]): Boolean =
    squares.forall(sq => !board.contains(sq))

  private def applyMoveUnchecked(state: ChessState, move: Move): ChessState =
    val side = state.sideToMove
    val piece = state.board(move.from)
    val captured = state.board.get(move.to)
    val isEnPassant =
      piece.kind == PieceType.Pawn && state.enPassant.contains(move.to) && captured.isEmpty

    val baseBoard = state.board - move.from

    val afterCapture =
      if isEnPassant then
        val capSq =
          Position(
            move.to.file,
            if side == Color.White then move.to.rank - 1 else move.to.rank + 1
          )
        // EP capture removes the pawn behind the target square.
        baseBoard - capSq
      else captured.fold(baseBoard)(_ => baseBoard - move.to)

    val movedPiece =
      (piece.kind, move.promotion) match
        case (PieceType.Pawn, Some(promotedTo)) => piece.copy(kind = promotedTo)
        case _                                 => piece

    val afterMove = afterCapture.updated(move.to, movedPiece)

    val isCastle = piece.kind == PieceType.King && (move.from.file - move.to.file).abs == 2
    val afterCastleRook =
      if !isCastle then afterMove
      else
        val rank = move.from.rank
        val rookShift: Option[(Position, Position)] =
          if move.to == Position('g', rank) then Some(Position('h', rank) -> Position('f', rank))
          else if move.to == Position('c', rank) then Some(Position('a', rank) -> Position('d', rank))
          else None

        rookShift
          .flatMap { case (rookFrom, rookTo) =>
            afterMove.get(rookFrom).map(r => (rookFrom, rookTo, r))
          }
          .map { case (rookFrom, rookTo, rook) =>
            (afterMove - rookFrom).updated(rookTo, rook)
          }
          .getOrElse(afterMove)

    val newEp =
      if piece.kind == PieceType.Pawn && (move.from.rank - move.to.rank).abs == 2 then
        Some(Position(move.from.file, (move.from.rank + move.to.rank) / 2))
      else None

    val newCastling = updateCastlingRights(state.castling, move, piece, state.board)

    val resetHalf =
      piece.kind == PieceType.Pawn || captured.isDefined || isEnPassant
    val half =
      if resetHalf then 0
      else state.halfmoveClock + 1

    val full =
      if side == Color.Black then state.fullmoveNumber + 1
      else state.fullmoveNumber

    ChessState(
      board = afterCastleRook,
      sideToMove = opposite(side),
      castling = newCastling,
      enPassant = newEp,
      halfmoveClock = half,
      fullmoveNumber = full,
      repetitionHistory = state.repetitionHistory,
      phase = GamePhase.InProgress
    )

  private def updateCastlingRights(
      rights: CastlingRights,
      move: Move,
      piece: Piece,
      boardBefore: Map[Position, Piece]
  ): CastlingRights =
    val afterKingMove =
      if piece.kind == PieceType.King then rights.without(piece.color) else rights

    def disableIfFrom(pos: Position)(update: CastlingRights => CastlingRights): CastlingRights => CastlingRights =
      r => if move.from == pos then update(r) else r

    def disableIfTo(pos: Position)(update: CastlingRights => CastlingRights): CastlingRights => CastlingRights =
      r => if move.to == pos then update(r) else r

    val rookMovedUpdates: List[CastlingRights => CastlingRights] =
      if piece.kind != PieceType.Rook then Nil
      else
        List(
          disableIfFrom(Position('h', 1))(_.copy(whiteKingside = false)),
          disableIfFrom(Position('a', 1))(_.copy(whiteQueenside = false)),
          disableIfFrom(Position('h', 8))(_.copy(blackKingside = false)),
          disableIfFrom(Position('a', 8))(_.copy(blackQueenside = false))
        )

    val rookCapturedUpdates: List[CastlingRights => CastlingRights] =
      boardBefore.get(move.to) match
        case Some(cap) if cap.kind == PieceType.Rook =>
          List(
            disableIfTo(Position('h', 1))(_.copy(whiteKingside = false)),
            disableIfTo(Position('a', 1))(_.copy(whiteQueenside = false)),
            disableIfTo(Position('h', 8))(_.copy(blackKingside = false)),
            disableIfTo(Position('a', 8))(_.copy(blackQueenside = false))
          )
        case _ => Nil

    (rookMovedUpdates ++ rookCapturedUpdates).foldLeft(afterKingMove) { (r, f) => f(r) }

end ChessRules
