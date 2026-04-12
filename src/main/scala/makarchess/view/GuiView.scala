package makarchess.view

import javafx.event.{ActionEvent as JfxActionEvent, EventHandler}
import makarchess.controller.ChessController
import makarchess.model.{ChessRules, ChessState, Position}
import makarchess.util.{MoveResult, Observer}

import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.Parent
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{BorderPane, GridPane, HBox, StackPane, VBox}
import scalafx.scene.paint.Color as SColor
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font
import scalafx.scene.input.MouseEvent

final class GuiView(controller: ChessController) extends Observer:

  controller.model.add(this)

  private val statusLabel = new Label("")
  private val currentPlayerLabel = new Label("")
  private val messageLabel = new Label("")

  private val inputField = new TextField:
    promptText = "e2e4"

  private val moveButton = new Button("Move")

  private val fenField = new TextField:
    promptText = "FEN string"

  private val fenButton = new Button("Load FEN")

  private val pgnField = new TextField:
    promptText = "PGN string"

  private val pgnButton = new Button("Load PGN")

  private val fileField = new TextField:
    promptText = "/path/to/file.fen or .pgn"

  private val fenFileButton = new Button("Load FEN File")

  private val pgnFileButton = new Button("Load PGN File")

  private val replayBackButton = new Button("<")

  private val replayForwardButton = new Button(">")

  private val replayLabel = new Label("Replay: inactive")

  private val boardGrid = new GridPane:
    hgap = 4
    vgap = 4
    padding = Insets(10)

  private var selectedFrom: Option[Position] = None

  private val rootPane = new BorderPane:
    padding = Insets(12)
    top = new VBox(6) {
      children = Seq(statusLabel, currentPlayerLabel, messageLabel)
    }
    center = boardGrid
    bottom = new VBox(8) {
      padding = Insets(8, 0, 0, 0)
      children = Seq(
        new HBox(8) {
          children = Seq(new Label("UCI:"), inputField, moveButton)
        },
        new HBox(8) {
          children = Seq(new Label("FEN:"), fenField, fenButton)
        },
        new HBox(8) {
          children = Seq(new Label("PGN:"), pgnField, pgnButton)
        },
        new HBox(8) {
          children = Seq(new Label("File:"), fileField, fenFileButton, pgnFileButton)
        },
        new HBox(8) {
          children = Seq(new Label("Replay:"), replayBackButton, replayForwardButton, replayLabel)
        }
      )
    }

  def root: Parent = rootPane

  moveButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitMove()

  inputField.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitMove()

  fenButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitFen()

  fenField.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitFen()

  pgnButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitPgn()

  pgnField.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = submitPgn()

  fenFileButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = loadFenFile()

  pgnFileButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = loadPgnFile()

  replayBackButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = stepReplayBackward()

  replayForwardButton.onAction = new EventHandler[JfxActionEvent]:
    override def handle(event: JfxActionEvent): Unit = stepReplayForward()

  private def submitMove(): Unit =
    val raw = inputField.text.value
    val uci = if raw == null then "" else raw.trim.toLowerCase
    if uci.nonEmpty then
      controller.handleMoveInput(uci) match
        case MoveResult.Ok(()) =>
          messageLabel.text = ""
          inputField.text = ""
          selectedFrom = None
        case MoveResult.Err(err) =>
          messageLabel.text = makarchess.model.ChessModel.formatError(err)

  private def submitFen(): Unit =
    val raw = fenField.text.value
    val fen = if raw == null then "" else raw.trim
    if fen.nonEmpty then
      controller.loadFenFromString(fen) match
        case Right(_) =>
          selectedFrom = None
          messageLabel.text = "FEN loaded."
          fenField.text = ""
        case Left(err) =>
          messageLabel.text = err

  private def submitPgn(): Unit =
    val raw = pgnField.text.value
    val pgn = if raw == null then "" else raw.trim
    if pgn.nonEmpty then
      controller.loadReplayFromPgnString(pgn) match
        case Right(_) =>
          selectedFrom = None
          messageLabel.text = "PGN replay loaded."
          pgnField.text = ""
        case Left(err) =>
          messageLabel.text = err

  private def loadFenFile(): Unit =
    val raw = fileField.text.value
    val path = if raw == null then "" else raw.trim
    if path.nonEmpty then
      controller.loadFenFromFile(path) match
        case Right(_) =>
          selectedFrom = None
          messageLabel.text = s"Loaded FEN file: $path"
        case Left(err) =>
          messageLabel.text = err

  private def loadPgnFile(): Unit =
    val raw = fileField.text.value
    val path = if raw == null then "" else raw.trim
    if path.nonEmpty then
      controller.loadReplayFromPgnFile(path) match
        case Right(_) =>
          selectedFrom = None
          messageLabel.text = s"Loaded PGN file: $path"
        case Left(err) =>
          messageLabel.text = err

  private def stepReplayBackward(): Unit =
    controller.stepReplayBackward() match
      case Right(_) =>
        selectedFrom = None
        messageLabel.text = ""
      case Left(err) =>
        messageLabel.text = err

  private def stepReplayForward(): Unit =
    controller.stepReplayForward() match
      case Right(_) =>
        selectedFrom = None
        messageLabel.text = ""
      case Left(err) =>
        messageLabel.text = err

  override def update: Unit =
    Platform.runLater {
      val s = controller.snapshot
      statusLabel.text = s.statusLine
      currentPlayerLabel.text = s.currentPlayerLine
      replayLabel.text =
        if controller.hasActiveReplay then
          s"Replay: ${controller.replayIndex.getOrElse(0)}/${controller.replayLength.getOrElse(0)}"
        else "Replay: inactive"
      replayBackButton.disable = !controller.hasActiveReplay || controller.replayIndex.contains(0)
      replayForwardButton.disable = !controller.hasActiveReplay || controller.replayIndex == controller.replayLength

      renderBoard(controller.model.chessState)
    }

  private def renderBoard(state: ChessState): Unit =
    boardGrid.children.clear()

    val squareSize = 56.0
    val pieceFont = Font.font("SansSerif", 36)
    val coordFont = Font.font("SansSerif", 14)

    val light = SColor.web("#EEEED2")
    val dark = SColor.web("#769656")

    def pieceGlyph(pos: Position): String =
      state.board.get(pos) match
        case None => ""
        case Some(p) => ChessRules.renderPiece(p).toString

    def squareClicked(pos: Position)(using MouseEvent): Unit =
      val current = controller.model.chessState
      val pieceOpt = current.board.get(pos)

      selectedFrom match
        case None =>
          pieceOpt match
            case None =>
              messageLabel.text = ""
            case Some(p) =>
              if p.color != current.sideToMove then
                messageLabel.text = "Not your turn for that piece."
              else
                selectedFrom = Some(pos)
                messageLabel.text = s"Selected ${pos.file}${pos.rank}"
                renderBoard(current)

        case Some(from) =>
          if from == pos then
            selectedFrom = None
            messageLabel.text = ""
            renderBoard(current)
          else
            val uci = s"${from.file}${from.rank}${pos.file}${pos.rank}".toLowerCase
            controller.handleMoveInput(uci) match
              case MoveResult.Ok(()) =>
                selectedFrom = None
                messageLabel.text = ""
              case MoveResult.Err(err) =>
                messageLabel.text = makarchess.model.ChessModel.formatError(err)
                selectedFrom = None
            renderBoard(controller.model.chessState)

    for rank <- 8 to 1 by -1 do
      val row = 8 - rank

      val rankLbl = new Label(rank.toString):
        font = coordFont
        minWidth = 18
      boardGrid.add(rankLbl, 0, row)

      for file <- 'a' to 'h' do
        val col = (file - 'a') + 1
        val pos = Position(file, rank)
        val isDark = ((file - 'a') + (rank - 1)) % 2 == 1

        val bg = new Rectangle:
          width = squareSize
          height = squareSize
          fill = if isDark then dark else light
          stroke =
            selectedFrom match
              case Some(sel) if sel == pos => SColor.web("#f6f669")
              case _                       => SColor.web("#333333")
          strokeWidth =
            selectedFrom match
              case Some(sel) if sel == pos => 3.0
              case _                       => 0.5

        val pieceLabel = new Label(pieceGlyph(pos)):
          font = pieceFont

        val cell = new StackPane:
          minWidth = squareSize
          minHeight = squareSize
          children = Seq(bg, pieceLabel)

        cell.onMouseClicked = (e: MouseEvent) =>
          squareClicked(pos)(using e)

        boardGrid.add(cell, col, row)

    for file <- 'a' to 'h' do
      val col = (file - 'a') + 1
      val fileLbl = new Label(file.toString):
        font = coordFont
      boardGrid.add(fileLbl, col, 8)

    val corner = new Label(""):
      minWidth = 18
    boardGrid.add(corner, 0, 8)
