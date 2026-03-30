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
    if s.statusLine.nonEmpty then io.printLine(s.statusLine)
    if s.currentPlayerLine.nonEmpty then io.printLine(s.currentPlayerLine)

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
