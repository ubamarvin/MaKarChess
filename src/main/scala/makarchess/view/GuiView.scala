package makarchess.view

import makarchess.controller.ChessController
import makarchess.model.{ChessRules, ChessState, Piece, Position}
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
    bottom = new HBox(8) {
      padding = Insets(8, 0, 0, 0)
      children = Seq(new Label("UCI:"), inputField, moveButton)
    }

  def root: Parent = rootPane

  moveButton.onAction = (_: scalafx.event.ActionEvent) =>
    submitMove()

  inputField.onAction = (_: scalafx.event.ActionEvent) =>
    submitMove()

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

  override def update: Unit =
    Platform.runLater {
      val s = controller.snapshot
      statusLabel.text = s.statusLine
      currentPlayerLabel.text = s.currentPlayerLine

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
