package makarchess.model

object PgnReplay:
  def buildCursor(startState: ChessState, moves: List[String]): Either[String, PgnReplayCursor] =
    moves.zipWithIndex.foldLeft[Either[String, Vector[ChessState]]](Right(Vector(ChessRules.evaluateState(startState)))) {
      case (acc, (token, index)) =>
        for
          states <- acc
          current = states.last
          move <- resolveSan(current, token).left.map(err => s"PGN move ${index + 1} ('$token'): $err")
          next = ChessRules.applyLegalMove(current, move)
        yield states :+ next
    }.map(states => PgnReplayCursor(states, 0))

  def toFinalState(startState: ChessState, moves: List[String]): Either[String, ChessState] =
    buildCursor(startState, moves).flatMap(_.jumpToEnd().currentState)

  def resolveSan(state: ChessState, token: String): Either[String, Move] =
    val normalized = normalizeSan(token)
    val matches =
      ChessRules.legalMoves(state).filter { move =>
        normalizeSan(renderSan(state, move)) == normalized
      }

    matches match
      case move :: Nil => Right(move)
      case Nil         => Left("could not match SAN to any legal move")
      case _           => Left("SAN is ambiguous in the current position")

  def renderSan(state: ChessState, move: Move): String =
    val movingPiece = state.board(move.from)

    if isCastle(move, movingPiece) then
      withCheckSuffix(state, move, if move.to.file == 'g' then "O-O" else "O-O-O")
    else
      val capture = isCapture(state, move, movingPiece)
      val piecePrefix =
        movingPiece.kind match
          case PieceType.Pawn   => ""
          case PieceType.Knight => "N"
          case PieceType.Bishop => "B"
          case PieceType.Rook   => "R"
          case PieceType.Queen  => "Q"
          case PieceType.King   => "K"

      val disambiguation = renderDisambiguation(state, move, movingPiece)
      val captureMarker = if capture then "x" else ""
      val target = s"${move.to.file}${move.to.rank}"
      val promotion = move.promotion.map(renderPromotion).getOrElse("")

      val san =
        movingPiece.kind match
          case PieceType.Pawn =>
            val pawnPrefix = if capture then move.from.file.toString else ""
            s"$pawnPrefix$captureMarker$target$promotion"
          case _ =>
            s"$piecePrefix$disambiguation$captureMarker$target$promotion"

      withCheckSuffix(state, move, san)

  private def renderDisambiguation(state: ChessState, move: Move, movingPiece: Piece): String =
    val similarMoves =
      ChessRules.legalMoves(state).filter { other =>
        other != move &&
        other.to == move.to &&
        state.board.get(other.from).contains(movingPiece) &&
        other.promotion == move.promotion
      }

    if similarMoves.isEmpty then ""
    else
      val sameFileExists = similarMoves.exists(_.from.file == move.from.file)
      val sameRankExists = similarMoves.exists(_.from.rank == move.from.rank)

      if !sameFileExists then move.from.file.toString
      else if !sameRankExists then move.from.rank.toString
      else s"${move.from.file}${move.from.rank}"

  private def withCheckSuffix(state: ChessState, move: Move, san: String): String =
    val next = ChessRules.applyLegalMove(state, move)
    next.phase match
      case GamePhase.Checkmate(_) => san + "#"
      case _ if ChessRules.isInCheck(next.board, next.sideToMove, next.enPassant) => san + "+"
      case _ => san

  private def isCastle(move: Move, movingPiece: Piece): Boolean =
    movingPiece.kind == PieceType.King && math.abs(move.to.file - move.from.file) == 2

  private def isCapture(state: ChessState, move: Move, movingPiece: Piece): Boolean =
    state.board.contains(move.to) ||
    (movingPiece.kind == PieceType.Pawn && state.enPassant.contains(move.to) && move.from.file != move.to.file)

  private def renderPromotion(pieceType: PieceType): String =
    pieceType match
      case PieceType.Queen  => "=Q"
      case PieceType.Rook   => "=R"
      case PieceType.Bishop => "=B"
      case PieceType.Knight => "=N"
      case _                => ""

  def normalizeSan(token: String): String =
    token.trim
      .replace("0-0-0", "O-O-O")
      .replace("0-0", "O-O")
      .replaceAll("[!?]+", "")
      .stripSuffix("+")
      .stripSuffix("#")
