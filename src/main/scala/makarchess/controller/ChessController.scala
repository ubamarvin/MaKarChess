package makarchess.controller

import makarchess.model.{ChessModel, GameSnapshot, MoveAttemptError, PieceType, Position}

/** Translates user intents into model operations. Does not own game state (see specs). */
class ChessController(initialModel: ChessModel):

  private var currentModel: ChessModel = initialModel

  def model: ChessModel =
    currentModel

  def snapshot: GameSnapshot =
    currentModel.snapshot

  def handleMoveInput(input: String): Either[MoveAttemptError, Unit] =
    currentModel.tryMove(input) match
      case Left(err) => Left(err)
      case Right(next) =>
        currentModel = next
        currentModel.notifyObservers
        Right(())

  def startNewGame(): Unit =
    currentModel = currentModel.restart()
    currentModel.notifyObservers

  def restartGame(): Unit =
    currentModel = currentModel.restart()
    currentModel.notifyObservers

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
    currentModel.tryMove(uci) match
      case Left(_)     => ()
      case Right(next) =>
        currentModel = next
        currentModel.notifyObservers
    ()

  private def pieceTypeToPromotionChar(pt: PieceType): Char =
    pt match
      case PieceType.Queen  => 'q'
      case PieceType.Rook   => 'r'
      case PieceType.Bishop => 'b'
      case PieceType.Knight => 'n'
      case _                => 'q'
