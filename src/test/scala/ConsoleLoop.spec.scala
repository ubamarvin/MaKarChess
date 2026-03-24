import munit.FunSuite
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
    runGame(io)

    assert(io.output.exists(_ == "White to move"))
    assertEquals(io.output.last, "Goodbye.")
  }

  test("runGame applies valid move and shows next player") {
    val io = FakeConsole(List("e2e4", "quit"))
    runGame(io)

    assert(io.output.contains("White to move"))
    assert(io.output.contains("Black to move"))
    assertEquals(io.output.last, "Goodbye.")
  }

  test("runGame reports parse and apply errors and keeps running") {
    val io = FakeConsole(List("bad", "e4e5", "quit"))
    runGame(io)

    assert(io.output.exists(_.startsWith("Error: Invalid move format")))
    assert(io.output.exists(_.startsWith("Error: No piece at source square")))
    assertEquals(io.output.last, "Goodbye.")
  }
