package makarchess.view

import makarchess.controller.ChessController
import makarchess.model.{ChessModel, ChessRules, ChessState, Color, Piece, PieceType}
import makarchess.util.Observer

trait ConsoleIO:
  def readLine(): Option[String]
  def printLine(line: String): Unit

class StdConsoleIO extends ConsoleIO:
  override def readLine(): Option[String] =
    Option(scala.io.StdIn.readLine())

  override def printLine(line: String): Unit =
    println(line)

/** TUI: observes the model via the shared observer contract; reads state through the controller. */
class TuiView(controller: ChessController, io: ConsoleIO) extends Observer:

  controller.model.add(this)

  override def update: Unit =
    val s = controller.snapshot
    val boardLines = s.boardText.split("\n").toList
    val capturedLines = capturedPanel(controller.model.chessState, boardLines.size)
    val merged = boardLines.zipAll(capturedLines, "", "").map { case (b, c) =>
      if c.isEmpty then b else s"$b   $c"
    }

    merged.foreach(io.printLine)

    controller.opponentModelStatusMessage.foreach(io.printLine)

    controller.opponentModelPrediction.foreach { p =>
      io.printLine("")
      io.printLine("Predictions")
      predictionBoard(p).foreach(io.printLine)
      predictionLegend(p).foreach(io.printLine)
    }

    controller.opponentModelStyleEstimate.foreach { est =>
      val side = controller.opponentModelModeledSide.map(_.toString.toLowerCase).getOrElse("?")
      io.printLine(
        f"Style (modeled $side): Agg ${est.aggressivePct}%.0f%% | Greed ${est.greedyPct}%.0f%% | Def ${est.defensivePct}%.0f%% | Risk ${est.riskTolerancePct}%.0f%% | Conf ${est.confidencePct}%.0f%%"
      )
    }

    if s.statusLine.nonEmpty then io.printLine(s.statusLine)
    if s.currentPlayerLine.nonEmpty then io.printLine(s.currentPlayerLine)

  private def predictionBoard(p: makarchess.opponentmodel.PredictionResult): List[String] =
    val state = controller.model.chessState
    val base = ChessRules.renderBoard(state.board).split("\n").toVector
    val highlights = p.highlights.map(h => (h.position, h.label)).toMap
    base
      .map { line =>
        val trimmed = line.stripLeading
        if trimmed.isEmpty then line
        else if !(trimmed.head.isDigit) then line
        else
          val rankChar = trimmed.head
          val rank = rankChar.asDigit
          val prefixLen = line.indexOf(rankChar) + 2
          val prefix = line.take(prefixLen)
          val rest = line.drop(prefixLen)
          val tokens = rest.split(" ").toVector
          val newTokens =
            ('a' to 'h').zipWithIndex.map { case (file, idx) =>
              val pos = makarchess.model.Position(file, rank)
              highlights.get(pos) match
                case None => tokens.lift(idx).getOrElse(".")
                case Some(lbl) => lbl
            }.mkString(" ")
          val suffix =
            if tokens.size >= 8 then " " + rankChar.toString
            else ""
          prefix + newTokens + suffix
      }
      .toList

  private def predictionLegend(p: makarchess.opponentmodel.PredictionResult): List[String] =
    val state = controller.model.chessState
    val entries =
      p.predictions.take(p.highlights.size).zipWithIndex.map { case (pm, idx) =>
        val label = (idx + 1).toString
        val pieceChar = state.board.get(pm.move.from).map(ChessRules.renderPiece).getOrElse('?')
        s"$label = $pieceChar ${pm.move.from.file}${pm.move.from.rank}${pm.move.to.file}${pm.move.to.rank}"
      }
    if entries.isEmpty then Nil
    else "Legend" :: entries.toList

  private def capturedPanel(state: ChessState, height: Int): List[String] =
    val (whiteCaptured, blackCaptured) = capturedPieces(state)

    val whiteLine = s"White captured: ${whiteCaptured.mkString}"
    val blackLine = s"Black captured: ${blackCaptured.mkString}"

    val base = List("Captured", whiteLine, blackLine)
    base ++ List.fill((height - base.size).max(0))("")

  private def capturedPieces(state: ChessState): (List[Char], List[Char]) =
    val initialCounts: Map[(Color, PieceType), Int] =
      val roles = List(
        PieceType.King,
        PieceType.Queen,
        PieceType.Rook,
        PieceType.Bishop,
        PieceType.Knight,
        PieceType.Pawn
      )

      def baseFor(color: Color)(kind: PieceType): Int =
        kind match
          case PieceType.King   => 1
          case PieceType.Queen  => 1
          case PieceType.Rook   => 2
          case PieceType.Bishop => 2
          case PieceType.Knight => 2
          case PieceType.Pawn   => 8

      (for
        color <- List(Color.White, Color.Black)
        kind <- roles
      yield (color -> kind) -> baseFor(color)(kind)).toMap

    val currentCounts: Map[(Color, PieceType), Int] =
      state.board.values.toList
        .groupBy(p => (p.color, p.kind))
        .view
        .mapValues(_.size)
        .toMap

    def missing(color: Color): List[PieceType] =
      val kinds = List(PieceType.Queen, PieceType.Rook, PieceType.Bishop, PieceType.Knight, PieceType.Pawn, PieceType.King)
      kinds.flatMap { kind =>
        val key = (color, kind)
        val init = initialCounts.getOrElse(key, 0)
        val now = currentCounts.getOrElse(key, 0)
        List.fill((init - now).max(0))(kind)
      }

    // Pieces missing from Black were captured by White, and vice versa.
    val whiteCaptured = missing(Color.Black).map(k => ChessRules.renderPiece(Piece(Color.Black, k)))
    val blackCaptured = missing(Color.White).map(k => ChessRules.renderPiece(Piece(Color.White, k)))
    (whiteCaptured, blackCaptured)

  /** Reads input, normalizes it, forwards actions to the controller (not the model directly). */
  def runInteractive(): Unit =
    @annotation.tailrec
    def loop(): Unit =
      io.printLine("Enter move (e.g. e2e4, e1g1, e7e8q) or 'quit':")
      io.readLine() match
        case None =>
          io.printLine("Goodbye.")
        case Some(raw) =>
          val input = raw.trim.toLowerCase
          if input == "quit" || input == "exit" then
            controller.quitGame()
            io.printLine("Goodbye.")
          else
            controller.handleMoveInput(input) match
              case Left(err) =>
                io.printLine(ChessModel.formatError(err))
                loop()
              case Right(()) =>
                loop()

    loop()
