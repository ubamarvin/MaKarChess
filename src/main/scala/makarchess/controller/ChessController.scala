package makarchess.controller

import makarchess.opponentmodel.HighlightSquare
import makarchess.opponentmodel.{PredictionResult, StyleEstimate}
import makarchess.model.{ChessModel, Color, GameSnapshot, MoveAttemptError, PieceType, Position}
import makarchess.util.MoveResult

/** Translates user intents into model operations. Does not own game state (see specs). */
class ChessController(initialModel: ChessModel):

  private var currentModel: ChessModel = initialModel

  private var opponentModelHighlightsState: Vector[HighlightSquare] = Vector.empty

  private var opponentModelObservedMovesState: Int = 0
  private var opponentModelModeledSideState: Option[Color] = None
  private var opponentModelStyleEstimateState: Option[StyleEstimate] = None
  private var opponentModelPredictionState: Option[PredictionResult] = None
  private var opponentModelStatusMessageState: Option[String] = None

  def model: ChessModel =
    currentModel

  def snapshot: GameSnapshot =
    currentModel.snapshot

  def opponentModelHighlights: Vector[HighlightSquare] =
    opponentModelHighlightsState

  def setOpponentModelHighlights(h: Vector[HighlightSquare]): Unit =
    opponentModelHighlightsState = h

  def opponentModelObservedMoves: Int =
    opponentModelObservedMovesState

  def setOpponentModelObservedMoves(n: Int): Unit =
    opponentModelObservedMovesState = n

  def opponentModelModeledSide: Option[Color] =
    opponentModelModeledSideState

  def setOpponentModelModeledSide(c: Option[Color]): Unit =
    opponentModelModeledSideState = c

  def opponentModelStyleEstimate: Option[StyleEstimate] =
    opponentModelStyleEstimateState

  def setOpponentModelStyleEstimate(s: Option[StyleEstimate]): Unit =
    opponentModelStyleEstimateState = s

  def opponentModelPrediction: Option[PredictionResult] =
    opponentModelPredictionState

  def setOpponentModelPrediction(p: Option[PredictionResult]): Unit =
    opponentModelPredictionState = p

  def opponentModelStatusMessage: Option[String] =
    opponentModelStatusMessageState

  def setOpponentModelStatusMessage(m: Option[String]): Unit =
    opponentModelStatusMessageState = m

  def handleMoveInput(input: String): MoveResult[Unit] =
    currentModel.tryMove(input) match
      case MoveResult.Err(err) => MoveResult.Err(err)
      case MoveResult.Ok(next) =>
        currentModel = next
        currentModel.notifyObservers
        MoveResult.pure(())

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
      case MoveResult.Err(_) => ()
      case MoveResult.Ok(next) =>
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
