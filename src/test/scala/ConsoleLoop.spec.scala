import munit.FunSuite
import makarchess.Main
import makarchess.model.ChessModel
import makarchess.view.ConsoleIO
import scala.collection.mutable.ListBuffer

class ConsoleLoopSpec extends FunSuite:
  private class FakeConsole(inputs: List[String]) extends ConsoleIO:
    private var remaining = inputs
    val output: ListBuffer[String] = ListBuffer.empty

    override def readLine(): Option[String] =
      remaining match
        case head :: tail =>
          remaining = tail
          Some(head)
        case Nil => None

    override def printLine(line: String): Unit =
      output += line

  test("runGame exits on quit command") {
    val io = FakeConsole(List("quit"))
    Main.run(ChessModel(), io)

    assert(io.output.exists(_ == "White to move"))
    assertEquals(io.output.last, "Goodbye.")
  }

  test("runGame applies valid move and shows next player") {
    val io = FakeConsole(List("e2e4", "quit"))
    Main.run(ChessModel(), io)

    assert(io.output.contains("White to move"))
    assert(io.output.contains("Black to move"))
    assertEquals(io.output.last, "Goodbye.")
  }

  test("runGame with bot enabled replies automatically") {
    val io = FakeConsole(List("e2e4", "quit"))
    Main.run(ChessModel(), io, botPlays = Some(makarchess.model.Color.Black))

    val whiteLines = io.output.count(_ == "White to move")
    assert(whiteLines >= 2)
    assert(io.output.contains("Black to move"))
    assertEquals(io.output.last, "Goodbye.")
  }

  test("runGame reports parse and apply errors and keeps running") {
    val io = FakeConsole(List("bad", "e4e5", "quit"))
    Main.run(ChessModel(), io)

    assert(io.output.exists(_.startsWith("Invalid move format")))
    assert(io.output.exists(_ == "Illegal move."))
    assertEquals(io.output.last, "Goodbye.")
  }
