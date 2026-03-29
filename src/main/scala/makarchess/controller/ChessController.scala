package makarchess.controller

import makarchess.model.{ChessModel, GameSnapshot, MoveAttemptError, PieceType, Position}

/** Translates user intents into model operations. Does not own game state (see specs). */
class ChessController(val model: ChessModel):

  def snapshot: GameSnapshot =
    model.snapshot

  def handleMoveInput(input: String): Either[MoveAttemptError, Unit] =
    model.tryMove(input)

  def startNewGame(): Unit =
    model.restart()

  def restartGame(): Unit =
    model.restart()

  def quitGame(): Unit = ()

  def selectSquare(pos: Position): Unit = ()

  def resign(): Unit = ()

  def offerDraw(): Unit = ()

  def acceptDraw(): Unit = ()

  def declineDraw(): Unit = ()

  def undo(): Unit = ()

  def redo(): Unit = ()

  def choosePromotion(pieceType: PieceType): Unit = ()

  def makeMove(from: Position, to: Position): Unit =
    makeMove(from, to, None)

  def makeMove(from: Position, to: Position, promotion: Option[PieceType]): Unit =
    val base = s"${from.file}${from.rank}${to.file}${to.rank}"
    val uci =
      promotion match
        case None    => base
        case Some(p) => base + pieceTypeToPromotionChar(p)
    model.tryMove(uci)
    ()

  private def pieceTypeToPromotionChar(pt: PieceType): Char =
    pt match
      case PieceType.Queen  => 'q'
      case PieceType.Rook   => 'r'
      case PieceType.Bishop => 'b'
      case PieceType.Knight => 'n'
      case _                => 'q'
