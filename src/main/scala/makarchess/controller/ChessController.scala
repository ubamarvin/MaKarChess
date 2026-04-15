package makarchess.controller

import makarchess.opponentmodel.HighlightSquare
import makarchess.opponentmodel.{PredictionResult, StyleEstimate}
import makarchess.model.{ChessModel, ChessRules, ChessState, Color, Fen, GameSnapshot, MoveAttemptError, PgnGame, PgnReplay, PgnReplayCursor, PieceType, Position}
import makarchess.parser.api.ParserBackend
import makarchess.parser.{FenParser, ParserModule, PgnParser}
import makarchess.persistence.{FenFileService, GameStateJsonService, LocalFileIO, PgnFileService}
import makarchess.serialization.UpickleGameStateJsonCodec
import makarchess.util.MoveResult

object ChessController:
  def apply(initialModel: ChessModel): ChessController =
    val fileIO = LocalFileIO()
    val fenParser = ParserModule.fenParser(ParserBackend.Fast)
    val pgnParser = ParserModule.pgnParser(ParserBackend.Fast)
    new ChessController(
      initialModel,
      fenParser,
      pgnParser,
      FenFileService(fileIO, fenParser),
      PgnFileService(fileIO, pgnParser),
      GameStateJsonService(fileIO, UpickleGameStateJsonCodec())
    )

/** Translates user intents into model operations. Does not own game state (see specs). */
class ChessController(
    initialModel: ChessModel,
    fenParser: FenParser,
    pgnParser: PgnParser,
    fenFileService: FenFileService,
    pgnFileService: PgnFileService,
    gameStateJsonService: GameStateJsonService
):

  private var currentModel: ChessModel = initialModel
  private var activeReplayCursor: Option[PgnReplayCursor] = None
  private var moveHistoryState: Vector[String] = Vector.empty
  private var currentPgnTextState: String = ""

  private def initialReplayState: ChessState =
    ChessRules.initialState

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

  def moveHistory: Vector[String] =
    moveHistoryState

  def currentPgnText: String =
    currentPgnTextState

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
        appendMoveHistoryFromUci(input)
        clearReplayState()
        currentModel = next
        currentModel.notifyObservers
        MoveResult.pure(())

  def loadFenFromString(input: String): Either[String, ChessState] =
    for
      fen <- fenParser.parse(input)
      state <- Fen.toChessState(fen)
    yield
      moveHistoryState = Vector.empty
      currentPgnTextState = ""
      clearReplayState()
      replaceModelState(state)

  def loadFenFromFile(path: String): Either[String, ChessState] =
    for
      fen <- fenFileService.load(path)
      state <- Fen.toChessState(fen)
    yield
      moveHistoryState = Vector.empty
      currentPgnTextState = ""
      clearReplayState()
      replaceModelState(state)

  def loadPgnFromString(input: String): Either[String, ChessState] =
    for
      pgn <- pgnParser.parse(input)
      cursor <- PgnReplay.buildCursor(initialReplayState, pgn.moves)
      state <- cursor.jumpToEnd().currentState
    yield
      moveHistoryState = pgn.moves.toVector
      currentPgnTextState = input.trim
      activeReplayCursor = Some(cursor.jumpToEnd())
      replaceModelState(state)

  def loadPgnFromFile(path: String): Either[String, ChessState] =
    for
      pgn <- pgnFileService.load(path)
      cursor <- PgnReplay.buildCursor(initialReplayState, pgn.moves)
      state <- cursor.jumpToEnd().currentState
    yield
      moveHistoryState = pgn.moves.toVector
      currentPgnTextState = pgnParser.render(pgn)
      activeReplayCursor = Some(cursor.jumpToEnd())
      replaceModelState(state)

  def parsePgnFromString(input: String): Either[String, PgnGame] =
    pgnParser.parse(input)

  def parsePgnFromFile(path: String): Either[String, PgnGame] =
    pgnFileService.load(path)

  def replayPgnFromString(input: String): Either[String, PgnReplayCursor] =
    for
      pgn <- pgnParser.parse(input)
      replay <- PgnReplay.buildCursor(initialReplayState, pgn.moves)
    yield replay

  def hasActiveReplay: Boolean =
    activeReplayCursor.nonEmpty

  def replayIndex: Option[Int] =
    activeReplayCursor.map(_.index)

  def replayLength: Option[Int] =
    activeReplayCursor.map(cursor => math.max(0, cursor.states.length - 1))

  def loadReplayFromPgnString(input: String): Either[String, ChessState] =
    for
      pgn <- pgnParser.parse(input)
      cursor <- replayPgnFromString(input)
      state <- cursor.currentState
    yield
      moveHistoryState = pgn.moves.toVector
      currentPgnTextState = input.trim
      activeReplayCursor = Some(cursor)
      replaceModelState(state)

  def loadReplayFromPgnFile(path: String): Either[String, ChessState] =
    for
      pgn <- pgnFileService.load(path)
      cursor <- PgnReplay.buildCursor(initialReplayState, pgn.moves)
      state <- cursor.currentState
    yield
      moveHistoryState = pgn.moves.toVector
      currentPgnTextState = pgnParser.render(pgn)
      activeReplayCursor = Some(cursor)
      replaceModelState(state)

  def stepReplayForward(): Either[String, ChessState] =
    activeReplayCursor match
      case None => Left("No PGN replay loaded.")
      case Some(cursor) =>
        for
          nextCursor <- cursor.stepForward()
          state <- nextCursor.currentState
        yield
          activeReplayCursor = Some(nextCursor)
          replaceModelState(state)

  def stepReplayBackward(): Either[String, ChessState] =
    activeReplayCursor match
      case None => Left("No PGN replay loaded.")
      case Some(cursor) =>
        for
          nextCursor <- cursor.stepBackward()
          state <- nextCursor.currentState
        yield
          activeReplayCursor = Some(nextCursor)
          replaceModelState(state)

  def jumpReplayToStart(): Either[String, ChessState] =
    activeReplayCursor match
      case None => Left("No PGN replay loaded.")
      case Some(cursor) =>
        val nextCursor = cursor.jumpToStart()
        nextCursor.currentState.map { state =>
          activeReplayCursor = Some(nextCursor)
          replaceModelState(state)
        }

  def jumpReplayToEnd(): Either[String, ChessState] =
    activeReplayCursor match
      case None => Left("No PGN replay loaded.")
      case Some(cursor) =>
        val nextCursor = cursor.jumpToEnd()
        nextCursor.currentState.map { state =>
          activeReplayCursor = Some(nextCursor)
          replaceModelState(state)
        }

  def saveCurrentStateToJson(): Either[String, Unit] =
    gameStateJsonService.save(currentModel.chessState)

  def loadStateFromJson(): Either[String, ChessState] =
    gameStateJsonService.load().map(replaceModelState)

  def startNewGame(): Unit =
    moveHistoryState = Vector.empty
    currentPgnTextState = ""
    clearReplayState()
    currentModel = currentModel.restart()
    currentModel.notifyObservers

  def restartGame(): Unit =
    moveHistoryState = Vector.empty
    currentPgnTextState = ""
    clearReplayState()
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
        appendMoveHistoryFromUci(uci)
        clearReplayState()
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

  private def replayPgn(pgn: PgnGame): Either[String, ChessState] =
    PgnReplay.toFinalState(currentModel.chessState, pgn.moves)

  private def clearReplayState(): Unit =
    activeReplayCursor = None

  private def appendMoveHistoryFromUci(input: String): Unit =
    ChessRules.parseUci(input) match
      case MoveResult.Ok(move) =>
        val san = PgnReplay.renderSan(currentModel.chessState, move)
        moveHistoryState = moveHistoryState :+ san
        currentPgnTextState = formatMoveHistory(moveHistoryState)
      case MoveResult.Err(_) =>
        ()

  private def formatMoveHistory(moves: Vector[String]): String =
    moves.grouped(2).zipWithIndex.map { case (pair, index) =>
      s"${index + 1}. ${pair.mkString(" ")}"
    }.mkString("\n")

  private def replaceModelState(state: ChessState): ChessState =
    currentModel = currentModel.withState(state)
    currentModel.notifyObservers
    state
